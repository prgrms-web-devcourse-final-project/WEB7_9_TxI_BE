package com.back.api.ticket.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.service.SeatService;
import com.back.api.ticket.dto.response.TicketResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.entity.TicketTransferHistory;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.ticket.repository.TicketTransferHistoryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.code.TicketErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.observability.metrics.BusinessMetrics;
import com.back.global.utils.MerkleUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 티켓 상태 변경 담당 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

	private final TicketRepository ticketRepository;
	private final TicketTransferHistoryRepository transferHistoryRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatService seatService;
	private final BusinessMetrics businessMetrics;

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

		Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		// 티켓 결제 확정 처리
		ticket.markPaid(); // 결제 성공
		ticket.issue();

		// 좌석 SOLD 처리 (원자적 업데이트)
		seatService.markSeatAsSold(ticket.getEvent().getId(), ticket.getSeat().getId());

		return ticket;
	}

	/**
	 * 결제 실패 → DRAFT 티켓 폐기 + 좌석 AVAILABLE 복구
	 */
	@Transactional
	public void failPayment(Long ticketId) {

		Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		// 티켓 실패 처리
		ticket.fail();

		// 좌석 해제 (원자적 업데이트)
		if (ticket.getSeat() != null) {
			seatService.markSeatAsAvailable(ticket.getEvent().getId(), ticket.getSeat().getId());
		}
	}

	/**
	 * 내 티켓 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<TicketResponse> getMyTickets(Long userId) {
		return ticketRepository.findMyTicketDto(userId);
	}

	public List<Ticket> getMyIssuedOrPaidTicketsBeforeEvent(Long userId) {
		return ticketRepository.findIssuedOrPaidBeforeEvent(userId, LocalDateTime.now());
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

	@Transactional(readOnly = true)
	public Ticket findById(Long ticketId) {
		return ticketRepository.findById(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	/**
	 * Draft만료 전용 스케줄러 사용하는 메소드
	 */
	@Transactional
	public void expireDraftTicket(Long ticketId) {
		Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		// 핵심 책임: 상태 변경은 무조건
		ticket.fail();

		// Draft 티켓 만료 메트릭
		businessMetrics.draftTicketExpired(ticket.getEvent().getId());

		// 부가 책임: 좌석이 있으면 해제 (원자적 업데이트)
		if (ticket.getSeat() != null) {
			seatService.markSeatAsAvailable(ticket.getEvent().getId(), ticket.getSeat().getId());
		}
	}

	public void releaseDraftTicketAndSeat(Long eventId, Long userId) {
		try {
			Optional<Ticket> draftTicketOpt =
				ticketRepository.findByEventIdAndOwnerIdAndTicketStatus(
					eventId, userId, TicketStatus.DRAFT
				);

			if (draftTicketOpt.isEmpty()) {
				return;
			}

			Ticket draftTicket = draftTicketOpt.get();
			draftTicket.cancel();

			if (draftTicket.getSeat() != null) {
				seatService.markSeatAsAvailable(eventId, draftTicket.getSeat().getId());
			}

		} catch (Exception e) {
			log.warn(
				"Draft ticket release failed on queue demotion (scheduler will handle) " +
					"- eventId={}, userId={}",
				eventId, userId, e
			);
		}
	}

	@Transactional
	public void transferTicket(Long ticketId, Long userId, String targetNickname) {
		// 비관락으로 조회
		Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TICKET_NOT_FOUND));

		if (!ticket.getOwner().getId().equals(userId)) {
			throw new ErrorException(TicketErrorCode.UNAUTHORIZED_TICKET_ACCESS);
		}

		User target = userRepository.findByNickname(targetNickname)
			.orElseThrow(() -> new ErrorException(TicketErrorCode.TRANSFER_TARGET_NOT_FOUND));

		Long fromUserId = ticket.getOwner().getId();

		// 양도 처리
		ticket.transferTo(target);

		// 양도 이력 저장
		TicketTransferHistory history = TicketTransferHistory.record(ticketId, fromUserId, target.getId());
		transferHistoryRepository.save(history);

		// Merkle Root 앵커링 (Loki로 전송됨)
		anchorTransferHistory(ticketId, history);

		log.debug("[Ticket Transfer] ticketId={}, from={}, to={}", ticketId, fromUserId, target.getId());
	}

	/**
	 * 양도 이력 Merkle Root 앵커링
	 * 블록체인의 핵심 원리(위변조 감지)를 차용하여
	 * 외부 로그 시스템(Loki)에 앵커링
	 */
	private void anchorTransferHistory(Long ticketId, TicketTransferHistory latestHistory) {
		List<TicketTransferHistory> histories =
			transferHistoryRepository.findByTicketIdOrderByTransferredAtDesc(ticketId);

		List<String> hashes = histories.stream()
			.map(TicketTransferHistory::computeHash)
			.toList();

		String merkleRoot = MerkleUtil.buildRoot(hashes);

		// 구조화된 로그 - Loki에서 파싱 가능 (외부 앵커)
		log.info("[MERKLE_ANCHOR] ticketId={}, root={}, count={}, latestHash={}",
			ticketId,
			merkleRoot,
			histories.size(),
			latestHistory.computeHash()
		);
	}
}
