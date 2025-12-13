package com.back.api.ticket.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.ticket.service.TicketService;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DraftTicketExpirationScheduler {

	private final TicketRepository ticketRepository;
	private final TicketService ticketService;

	@Scheduled(fixedRate = 60_000) // 1분마다 실행
	public void expireDraftTickets() {

		LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(15);

		List<Ticket> expiredTickets =
			ticketRepository.findExpiredDraftTickets(TicketStatus.DRAFT, expiredBefore);

		for (Ticket ticket : expiredTickets) {
			try {
				ticketService.failPayment(ticket.getId());
				log.info("만료된 Draft Ticket 처리 완료: ticketId={}", ticket.getId());
			} catch (Exception ex) {
				log.error("Draft 티켓 만료 처리 실패 - ticketId={}", ticket.getId(), ex);
			}
		}
	}
}
