package com.back.api.ticket.scheduler;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.back.api.ticket.service.TicketService;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.global.logging.MdcContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class DraftTicketExpirationScheduler {

	private static final int PAGE_SIZE = 500;
	private static final int MAX_PER_RUN = 2000;

	private final TicketRepository ticketRepository;
	private final TicketService ticketService;

	@Scheduled(fixedRate = 60_000)
	@SchedulerLock(
		name = "DraftTicketExpiration",
		lockAtMostFor = "5m",
		lockAtLeastFor = "10s"
	)
	public void expireDraftTickets() {
		expireDraftTicketsInternal();
	}

	/**
	 *  NOTE:
	 *  ShedLock AOP가 테스트 환경에서 스케줄러 실행을 막기 때문에
	 *  테스트에서는 expireDraftTicketsInternal()을 직접 호출한다.
	 *  운영 환경에서는 반드시 expireDraftTickets()만 사용해야 한다.
	 */
	public void expireDraftTicketsInternal() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		try {
			MdcContext.putRunId(runId);
			LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(15);

			log.info(
				"SCHED_START job=DraftTicketExpiration expiredBefore={}",
				expiredBefore
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
						ticketService.expireDraftTicket(ticket.getId());
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
				"SCHED_END job=DraftTicketExpiration total={} success={} fail={} durationMs={}",
				total, success, fail, durationMs
			);

		} catch (Exception ex) {
			long durationMs = System.currentTimeMillis() - startAt;
			log.error(
				"SCHED_FAIL job=DraftTicketExpiration durationMs={} error={}",
				durationMs, ex.toString(), ex
			);
		} finally {
			MdcContext.removeRunId();
		}
	}
}
