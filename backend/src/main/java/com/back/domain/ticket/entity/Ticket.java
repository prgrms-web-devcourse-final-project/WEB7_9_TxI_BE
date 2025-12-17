package com.back.domain.ticket.entity;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;

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
	@JoinColumn(name = "seat_id", nullable = true)
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Enumerated(EnumType.STRING)
	@Column(name = "ticket_status", nullable = false)
	private TicketStatus ticketStatus;

	@Column(name = "issued_at")
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

	/**
	 * 테스트 데이터 생성용 티켓 생성 (상태 전이 없이 바로 ISSUED)
	 * 실제 비즈니스 로직을 거치지 않고 테스트 데이터를 생성하기 위한 전용 메서드
	 */
	public static Ticket issuedForPerf(User owner, Seat seat, Event event) {
		Ticket ticket = new Ticket();
		ticket.owner = owner;
		ticket.seat = seat;
		ticket.event = event;
		ticket.ticketStatus = TicketStatus.ISSUED;
		ticket.issuedAt = LocalDateTime.now();
		return ticket;
	}

	// DRAFT → PAID
	public void markPaid() {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.ticketStatus = TicketStatus.PAID;
	}

	// PAID → ISSUED
	public void issue() {
		if (this.ticketStatus != TicketStatus.PAID) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.ticketStatus = TicketStatus.ISSUED;
		this.issuedAt = LocalDateTime.now();
	}

	// ISSUED → USED
	public void markAsUsed() {
		if (this.ticketStatus != TicketStatus.ISSUED) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.ticketStatus = TicketStatus.USED;
		this.usedAt = LocalDateTime.now();
	}

	// DRAFT → FAILED (결제 실패)
	public void fail() {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.ticketStatus = TicketStatus.FAILED;
	}

	// DRAFT → CANCELLED (사용자 취소)
	public void cancel() {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.ticketStatus = TicketStatus.CANCELLED;
	}

	/**
	 * 좌석 할당 (DRAFT 티켓에만 가능)
	 */
	public void assignSeat(Seat seat) {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.seat = seat;
	}

	/**
	 * 좌석 해제 (DRAFT 티켓에만 가능)
	 */
	public void clearSeat() {
		if (this.ticketStatus != TicketStatus.DRAFT) {
			throw new ErrorException(TicketErrorCode.INVALID_TICKET_STATE);
		}
		this.seat = null;
	}

	/**
	 * 좌석 할당 여부 확인
	 */
	public boolean hasSeat() {
		return this.seat != null;
	}
}
