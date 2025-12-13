package com.back.support.helper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.support.factory.TicketFactory;

@Component
public class TicketHelper {

	private final TicketRepository ticketRepository;

	public TicketHelper(TicketRepository ticketRepository) {
		this.ticketRepository = ticketRepository;
	}

	/**
	 * Draft Ticket 저장
	 */
	public Ticket createDraftTicket(User owner, Seat seat, Event event) {
		Ticket ticket = TicketFactory.fakeDraftTicket(owner, seat, event);
		return ticketRepository.save(ticket);
	}

	/**
	 * Paid Ticket 저장
	 */
	public Ticket createPaidTicket(User owner, Seat seat, Event event) {
		Ticket ticket = TicketFactory.fakePaidTicket(owner, seat, event);
		return ticketRepository.save(ticket);
	}

	/**
	 * Issued Ticket 저장
	 */
	public Ticket createIssuedTicket(User owner, Seat seat, Event event) {
		Ticket ticket = TicketFactory.fakeIssuedTicket(owner, seat, event);
		return ticketRepository.save(ticket);
	}

	/**
	 * Failed Ticket 저장
	 */
	public Ticket createFailedTicket(User owner, Seat seat, Event event) {
		Ticket ticket = TicketFactory.fakeFailedTicket(owner, seat, event);
		return ticketRepository.save(ticket);
	}

	/**
	 * 여러 개 티켓 생성
	 */
	public List<Ticket> createTickets(User owner, Event event, List<Seat> seats, TicketStatus status) {
		return ticketRepository.saveAll(
			seats.stream()
				.map(seat -> Ticket.builder()
					.owner(owner)
					.seat(seat)
					.event(event)
					.ticketStatus(status)
					.build())
				.toList()
		);
	}

	public void clearTickets() {
		ticketRepository.deleteAll();
	}
}
