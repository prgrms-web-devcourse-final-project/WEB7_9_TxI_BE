package com.back.domain.ticket.repository;

import static com.back.domain.event.entity.QEvent.*;
import static com.back.domain.seat.entity.QSeat.*;
import static com.back.domain.ticket.entity.QTicket.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryImpl implements TicketRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public List<TicketResponse> findMyTicketDto(Long userId) {
		return jpaQueryFactory
			.select(Projections.constructor(
				TicketResponse.class,
				ticket.id,
				event.id,
				event.title,
				seat.seatCode,
				seat.grade,
				seat.price,
				seat.seatStatus,
				ticket.ticketStatus,
				ticket.issuedAt,
				ticket.usedAt
			))
			.from(ticket)
			.join(ticket.event, event)
			.leftJoin(ticket.seat, seat)
			.where(
				ticket.owner.id.eq(userId),
				ticket.ticketStatus.in(
					TicketStatus.PAID,
					TicketStatus.ISSUED,
					TicketStatus.USED
				)
			)
			.orderBy(ticket.createAt.desc())
			.fetch();

	}

	@Override
	public List<Ticket> findIssuedOrPaidBeforeEvent(Long userId, LocalDateTime now) {
		return jpaQueryFactory
			.selectFrom(ticket)
			.join(ticket.event, event).fetchJoin()
			.where(
				ticket.owner.id.eq(userId),
				ticket.ticketStatus.in(TicketStatus.ISSUED, TicketStatus.PAID),
				event.eventDate.after(now)
			)
			.fetch();
	}
}
