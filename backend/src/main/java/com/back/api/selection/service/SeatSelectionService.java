package com.back.api.selection.service;

import org.springframework.stereotype.Service;

import com.back.domain.ticket.entity.Ticket;
import com.back.global.lock.DistributedLockManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좌석 선택 + DraftTicket 생성 서비스
 * - Redisson 분산 락으로 좌석 선택 동시성 제어
 * - 성능 최적화: 락 시간 최소화 (100ms 대기, 500ms 보유)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatSelectionService {

	private final SeatSelectionExecutor executor;
	private final DistributedLockManager lockManager;

	/**
	 * 좌석 선택 + DraftTicket 생성/업데이트
	 * - 기존 Draft가 있으면 재사용 (좌석만 변경)
	 * - 없으면 새로 생성
	 *
	 * 안전성 보장: 새 좌석 예약 성공 후에만 기존 좌석 해제
	 * 동시성 제어: Redisson 분산 락 + DB 원자적 업데이트
	 * 성능 최적화: 락 시간 최소화로 처리량 극대화
	 */
	public Ticket selectSeatAndCreateTicket(Long eventId, Long seatId, Long userId) {
		String lockKey = DistributedLockManager.generateSeatLockKey(eventId, seatId);
		return lockManager.executeWithLock(lockKey, () -> executor.selectSeat(eventId, seatId, userId));
	}

	/**
	 * 좌석 선택 취소 (DraftTicket은 유지, 좌석만 해제)
	 */
	public void deselectSeatAndCancelTicket(Long eventId, Long seatId, Long userId) {
		executor.deselectSeat(eventId, seatId, userId);
	}
}
