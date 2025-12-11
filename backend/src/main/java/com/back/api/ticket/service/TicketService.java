package com.back.api.ticket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;

	/**
	 * Draft Ticket 생성 (좌석 선택 직후)
	 *  - 결제 전 임시 티켓
	 *  - 좌석 RESERVED 상태 이후에 호출됨
	 */
	public Ticket createDraftTicket(Long eventId, Long seatId, Long userId) {

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(CommonErrorCode.NOT_FOUND_USER));

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		// 이미 Draft Ticket이 존재하면 재사용 or 에러 처리
		if (ticketRepository.existsBySeatIdAndTicketStatus(seatId, TicketStatus.DRAFT)) {
			throw new ErrorException(TicketErrorCode.TICKET_ALREADY_IN_PROGRESS);
		}

		Ticket ticket = Ticket.builder()
			.owner(user)
			.event(event)
			.seat(seat)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		return ticketRepository.save(ticket);
	}

	/**
	 * 결제 완료 → Ticket 확정 발급
	 */
	public Ticket confirmPayment(Long ticketId, Long userId) {

		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		Seat seat = ticket.getSeat();

		seat.markAsSold(); // 좌석 SOLD 처리
		ticket.markPaid(); // 결제 성공

		// 여기서 실제 ISSUED 시점은 옵션
		ticket.issue();

		return ticket;
	}

	/**
	 * 결제 실패 → DRAFT 티켓 폐기 + 좌석 AVAILABLE 복구
	 */
	public void failPayment(Long ticketId) {

		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		Seat seat = ticket.getSeat();

		// 좌석 잠금 해제
		seat.markAsAvailable();

		// 티켓 실패 처리
		ticket.fail();
	}

	/**
	 * 내 티켓 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<Ticket> getMyTickets(Long userId) {
		return ticketRepository.findByOwnerId(userId);
	}

	/**
	 * 티켓 상세 조회
	 */
	@Transactional(readOnly = true)
	public Ticket getTicketDetail(Long ticketId, Long userId) {

		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		return ticket;
	}
}
