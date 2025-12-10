package com.back.domain.ticket.entity;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Ticket extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq")
	@SequenceGenerator(
		name = "ticket_seq",
		sequenceName = "ticket_seq",
		allocationSize = 100
	)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Enumerated(EnumType.STRING)
	@Column(name = "ticket_status", nullable = false)
	private TicketStatus ticketStatus;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	public static Ticket issue(User owner, Seat seat, Event event, String verificationHash) {
		Ticket ticket = new Ticket();
		ticket.owner = owner;
		ticket.seat = seat;
		ticket.event = event;
		ticket.ticketStatus = TicketStatus.ISSUED;
		ticket.issuedAt = LocalDateTime.now();
		return ticket;
	}

	public void markAsUsed() {
		this.ticketStatus = TicketStatus.USED;
		this.usedAt = LocalDateTime.now();
	}

	public void markPaid() {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new IllegalStateException();
		}
		this.ticketStatus = TicketStatus.PAID;
	}

	public void issue() {
		if (this.ticketStatus != TicketStatus.PAID) {
			throw new IllegalStateException();
		}
		this.ticketStatus = TicketStatus.ISSUED;
		this.issuedAt = LocalDateTime.now();
	}

	public void fail() {
		this.ticketStatus = TicketStatus.FAILED;
	}
}
