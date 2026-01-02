package com.back.api.selection.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.service.SeatService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.seat.entity.Seat;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 선택 트랜잭션 실행 서비스
 * - SeatSelectionService에서 분리하여 @Transactional이 정상 작동하도록 함
 * - 성능 최적화: 트랜잭션 범위 최소화, 중복 조회 제거
 */
@Service
@RequiredArgsConstructor
public class SeatSelectionExecutor {

	private final SeatService seatService;
	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final QueueEntryReadService queueEntryReadService;

	/**
	 * 좌석 선택 트랜잭션 실행 (분산 락 내부에서 호출됨)
	 * 성능 최적화: 트랜잭션을 짧게 유지
	 */
	@Transactional
	public Ticket selectSeat(Long eventId, Long seatId, Long userId) {
		// 큐 검증 (빠른 실패)
		if (!queueEntryReadService.isUserEntered(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		// Draft Ticket 조회 또는 생성 (1개 보장)
		Ticket ticket = ticketService.getOrCreateDraft(eventId, userId);
		Seat oldSeat = ticket.getSeat();

		// 새 좌석 먼저 예약 (실패 시 기존 좌석 유지)
		// 이미 원자적 업데이트로 동시성 제어됨 (updateSeatStatusIfMatch)
		Seat newSeat = seatService.reserveSeat(eventId, seatId, userId);

		// Ticket에 좌석 할당
		ticket.assignSeat(newSeat);

		// 변경 사항을 명시적으로 저장 (JPA dirty checking 보장)
		ticketRepository.save(ticket);

		// 새 좌석 예약 성공했을 때만 기존 좌석 해제
		if (oldSeat != null) {
			seatService.markSeatAsAvailable(oldSeat);
		}

		return ticket;
	}

	/**
	 * 좌석 선택 취소 (DraftTicket은 유지, 좌석만 해제)
	 * 성능 최적화: 트랜잭션을 짧게 유지
	 */
	@Transactional
	public void deselectSeat(Long eventId, Long seatId, Long userId) {
		// Draft Ticket 조회
		Ticket ticket = ticketService.getOrCreateDraft(eventId, userId);

		// 좌석 검증 (빠른 실패)
		if (!ticket.hasSeat() || !ticket.getSeat().getId().equals(seatId)) {
			throw new ErrorException(SeatErrorCode.SEAT_NOT_SELECTED);
		}

		// 좌석 해제
		Seat seat = ticket.getSeat();
		seatService.markSeatAsAvailable(seat);

		// Ticket에서 좌석 제거 (티켓은 유지)
		ticket.clearSeat();

		// 변경 사항을 명시적으로 저장 (JPA dirty checking 보장)
		ticketRepository.save(ticket);
	}
}
