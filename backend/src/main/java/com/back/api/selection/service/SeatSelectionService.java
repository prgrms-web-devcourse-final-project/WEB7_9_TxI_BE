package com.back.api.selection.service;

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

/**
 * 좌석 선택 + DraftTicket 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class SeatSelectionService {

	private final SeatService seatService;
	private final TicketService ticketService;
	private final QueueEntryReadService queueEntryReadService;

	/**
	 * 좌석 선택 + DraftTicket 생성/업데이트
	 * - 기존 Draft가 있으면 재사용 (좌석만 변경)
	 * - 없으면 새로 생성
	 */
	@Transactional
	public Ticket selectSeatAndCreateTicket(Long eventId, Long seatId, Long userId) {
		if (!queueEntryReadService.isUserEntered(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		// Draft Ticket 조회 또는 생성 (1개 보장)
		Ticket ticket = ticketService.getOrCreateDraft(eventId, userId);

		// 기존 좌석이 있으면 먼저 해제
		if (ticket.hasSeat()) {
			seatService.markSeatAsAvailable(ticket.getSeat());
		}

		// 새 좌석 예약
		Seat newSeat = seatService.reserveSeat(eventId, seatId, userId);

		// Ticket에 좌석 할당
		ticket.assignSeat(newSeat);

		return ticket;
	}

	/**
	 * 좌석 선택 취소 (DraftTicket은 유지, 좌석만 해제)
	 */
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
