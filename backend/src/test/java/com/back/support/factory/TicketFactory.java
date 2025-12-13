package com.back.support.factory;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;

public class TicketFactory extends BaseFactory {

	/**
	 * 기본 Draft Ticket 생성 (저장 X)
	 */
	public static Ticket fakeDraftTicket(User owner, Seat seat, Event event) {
		return Ticket.builder()
			.owner(owner)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.DRAFT)
			.build();
	}

	/**
	 * Issued Ticket 생성 (저장 X)
	 */
	public static Ticket fakeIssuedTicket(User owner, Seat seat, Event event) {
		return Ticket.builder()
			.owner(owner)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.ISSUED)
			.issuedAt(LocalDateTime.now().minusMinutes(faker.number().numberBetween(1, 30)))
			.build();
	}

	/**
	 * Paid Ticket 생성 (저장 X)
	 */
	public static Ticket fakePaidTicket(User owner, Seat seat, Event event) {
		return Ticket.builder()
			.owner(owner)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.PAID)
			.build();
	}

	/**
	 * 실패 티켓
	 */
	public static Ticket fakeFailedTicket(User owner, Seat seat, Event event) {
		return Ticket.builder()
			.owner(owner)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.FAILED)
			.build();
	}
}