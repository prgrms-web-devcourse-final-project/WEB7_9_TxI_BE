package com.back.api.ticket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.service.SeatService;
import com.back.api.ticket.dto.response.TicketResponse;
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

/**
 * 티켓 상태 변경 담당 서비스
 */
@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;
	private final SeatService seatService;

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

		// 좌석에 이미 PAID or ISSUED 티켓 있으면 안 됨
		if (ticketRepository.existsBySeatIdAndTicketStatusIn(
			seatId,
			List.of(TicketStatus.PAID, TicketStatus.ISSUED))
		) {
			throw new ErrorException(TicketErrorCode.SEAT_ALREADY_PURCHASED);
		}

		// 이미 Draft Ticket이 존재하면 중복 생성 금지, 예외처리
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
	 * Draft Ticket 조회 또는 생성 (유저+이벤트당 1개 유지)
	 * - 기존 Draft가 있으면 반환
	 * - 없으면 새로 생성 (좌석 없이)
	 */
	@Transactional
	public Ticket getOrCreateDraft(Long eventId, Long userId) {
		return ticketRepository
			.findByEventIdAndOwnerIdAndTicketStatus(eventId, userId, TicketStatus.DRAFT)
			.orElseGet(() -> {
				User user = userRepository.findById(userId)
					.orElseThrow(() -> new ErrorException(CommonErrorCode.NOT_FOUND_USER));

				Event event = eventRepository.findById(eventId)
					.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

				Ticket ticket = Ticket.builder()
					.owner(user)
					.event(event)
					.seat(null)  // 좌석 없이 생성
					.ticketStatus(TicketStatus.DRAFT)
					.build();

				return ticketRepository.save(ticket);
			});
	}

	/**
	 * 진행 중인 Draft Ticket 조회
	 */
	@Transactional(readOnly = true)
	public Ticket getDraftTicket(Long eventId, Long seatId, Long userId) {
		Ticket ticket = ticketRepository.findBySeatIdAndOwnerIdAndTicketStatus(
			seatId,
			userId,
			TicketStatus.DRAFT
		).orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_IN_PROGRESS));

		// 티켓이 해당 이벤트에 속하는지 검증
		if (!ticket.getEvent().getId().equals(eventId)) {
			throw new ErrorException(TicketErrorCode.TICKET_EVENT_MISMATCH);
		}

		return ticket;
	}

	/**
	 * 결제 완료 → Ticket 확정 발급
	 */
	@Transactional
	public Ticket confirmPayment(Long ticketId, Long userId) {

		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		// 티켓 결제 확정 처리
		ticket.markPaid(); // 결제 성공
		ticket.issue();

		seatService.markSeatAsSold(ticket.getSeat()); // 좌석 SOLD 처리

		return ticket;
	}

	/**
	 * 결제 실패 → DRAFT 티켓 폐기 + 좌석 AVAILABLE 복구
	 */
	@Transactional
	public void failPayment(Long ticketId) {

		Ticket ticket = ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		// 티켓 실패 처리
		ticket.fail();

		// 좌석 해제
		if (ticket.getSeat() != null) {
			seatService.markSeatAsAvailable(ticket.getSeat());
		}
	}

	/**
	 * 내 티켓 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<TicketResponse> getMyTickets(Long userId) {
		return ticketRepository.findMyTicketDto(userId);
	}

	/**
	 * 티켓 상세 조회
	 */
	@Transactional(readOnly = true)
	public Ticket getTicketDetail(Long ticketId, Long userId) {

		Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		return ticket;
	}
}
