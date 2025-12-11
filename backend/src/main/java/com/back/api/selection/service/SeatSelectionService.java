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
	 * 좌석 선택 + DraftTicket 생성
	 */
	@Transactional
	public Ticket selectSeatAndCreateTicket(Long eventId, Long seatId, Long userId) {
		if (!queueEntryReadService.isUserEntered(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		Seat reservedSeat = seatService.reserveSeat(eventId, seatId, userId);

		Ticket draftTicket = ticketService.createDraftTicket(eventId, seatId, userId);

		return draftTicket;
	}
}
