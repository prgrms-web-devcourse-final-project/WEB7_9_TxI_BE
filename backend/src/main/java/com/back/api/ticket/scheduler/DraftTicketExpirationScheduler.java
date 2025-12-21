package com.back.api.ticket.scheduler;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

	private static final int PAGE_SIZE = 500;
	private static final int MAX_PER_RUN = 2000;

	private final TicketRepository ticketRepository;
	private final TicketService ticketService;

	@Scheduled(fixedRate = 60_000)
	public void expireDraftTickets() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		try {
			LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(15);

			log.info(
				"SCHED_START job=DraftTicketExpiration runId={} expiredBefore={}",
				runId, expiredBefore
			);

			int total = 0;
			int success = 0;
			int fail = 0;
			int page = 0;

			while (total < MAX_PER_RUN) {
				Pageable pageable = PageRequest.of(page, PAGE_SIZE);
				Page<Ticket> ticketPage =
					ticketRepository.findExpiredDraftTickets(
						TicketStatus.DRAFT, expiredBefore, pageable
					);

				if (ticketPage.isEmpty()) {
					break;
				}

				for (Ticket ticket : ticketPage.getContent()) {
					try {
						ticketService.failPayment(ticket.getId());
						success++;
						log.debug(
							"SCHED_ITEM_SUCCESS job=DraftTicketExpiration runId={} ticketId={}",
							runId, ticket.getId()
						);
					} catch (Exception ex) {
						fail++;
						log.error(
							"SCHED_ITEM_FAIL job=DraftTicketExpiration runId={} ticketId={} error={}",
							runId, ticket.getId(), ex.toString(), ex
						);
					}
				}

				total += ticketPage.getNumberOfElements();
				page++;

				if (total >= MAX_PER_RUN) {
					log.warn(
						"SCHED_LIMIT_REACHED job=DraftTicketExpiration runId={} limit={}",
						runId, MAX_PER_RUN
					);
					break;
				}
			}

			long durationMs = System.currentTimeMillis() - startAt;
			log.info(
				"SCHED_END job=DraftTicketExpiration runId={} total={} success={} fail={} durationMs={}",
				runId, total, success, fail, durationMs
			);

		} catch (Exception ex) {
			long durationMs = System.currentTimeMillis() - startAt;
			log.error(
				"SCHED_FAIL job=DraftTicketExpiration runId={} durationMs={} error={}",
				runId, durationMs, ex.toString(), ex
			);
		}
	}
}
