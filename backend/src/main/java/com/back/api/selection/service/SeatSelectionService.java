package com.back.api.selection.service;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.service.SeatService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.seat.entity.Seat;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좌석 선택 + DraftTicket 생성 서비스
 * - Redisson 분산 락으로 좌석 선택 동시성 제어
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatSelectionService {

	private final SeatService seatService;
	private final TicketService ticketService;
	private final QueueEntryReadService queueEntryReadService;
	private final RedissonClient redissonClient;

	private static final long LOCK_WAIT_TIME = 3L;  // 락 대기 시간 (초)
	private static final long LOCK_LEASE_TIME = 5L; // 락 보유 시간 (초)

	/**
	 * 좌석 선택 + DraftTicket 생성/업데이트
	 * - 기존 Draft가 있으면 재사용 (좌석만 변경)
	 * - 없으면 새로 생성
	 *
	 * 안전성 보장: 새 좌석 예약 성공 후에만 기존 좌석 해제
	 * 동시성 제어: Redisson 분산 락으로 좌석별 동시 접근 방지
	 */
	public Ticket selectSeatAndCreateTicket(Long eventId, Long seatId, Long userId) {
		String lockKey = generateSeatLockKey(eventId, seatId);
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 락 획득 시도 (대기 시간: 3초, 보유 시간: 5초)
			boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
			if (!acquired) {
				log.warn("Failed to acquire lock for seat. eventId={}, seatId={}, userId={}",
					eventId, seatId, userId);
				throw new ErrorException(SeatErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
			}

			// 락 획득 후 트랜잭션 실행
			return executeSelectSeat(eventId, seatId, userId);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Lock acquisition interrupted. eventId={}, seatId={}, userId={}",
				eventId, seatId, userId, e);
			throw new ErrorException(SeatErrorCode.SEAT_LOCK_INTERRUPTED);
		} finally {
			// 락 해제 (현재 스레드가 보유한 경우만)
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	// 좌석 선택 트랜잭션 실행 (분산 락 내부에서 실행)
	@Transactional
	protected Ticket executeSelectSeat(Long eventId, Long seatId, Long userId) {
		if (!queueEntryReadService.isUserEntered(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		// Draft Ticket 조회 또는 생성 (1개 보장)
		Ticket ticket = ticketService.getOrCreateDraft(eventId, userId);
		Seat oldSeat = ticket.getSeat();

		// 새 좌석 먼저 예약 (실패 시 기존 좌석 유지)
		Seat newSeat = seatService.reserveSeat(eventId, seatId, userId);

		// Ticket에 좌석 할당
		ticket.assignSeat(newSeat);

		// 새 좌석 예약 성공했을 때만 기존 좌석 해제
		if (oldSeat != null) {
			seatService.markSeatAsAvailable(oldSeat);
		}

		return ticket;
	}

	// 좌석 락 키 생성
	private String generateSeatLockKey(Long eventId, Long seatId) {
		return "seat:lock:" + eventId + ":" + seatId;
	}

	// 좌석 선택 취소 (DraftTicket은 유지, 좌석만 해제)
	@Transactional
	public void deselectSeatAndCancelTicket(Long eventId, Long seatId, Long userId) {
		// Draft Ticket 조회
		Ticket ticket = ticketService.getOrCreateDraft(eventId, userId);

		// 좌석 검증
		if (!ticket.hasSeat() || !ticket.getSeat().getId().equals(seatId)) {
			throw new ErrorException(SeatErrorCode.SEAT_NOT_SELECTED);
		}

		// 좌석 해제
		Seat seat = ticket.getSeat();
		seatService.markSeatAsAvailable(seat);

		// Ticket에서 좌석 제거 (티켓은 유지)
		ticket.clearSeat();
	}
}
