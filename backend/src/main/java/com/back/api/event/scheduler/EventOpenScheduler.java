package com.back.api.event.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.global.logging.MdcContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "perf"})
public class EventOpenScheduler {

	private final EventRepository eventRepository;

	@Scheduled(cron = "${event.scheduler.open.cron}", zone = "Asia/Seoul") // 매 분 실행
	@SchedulerLock(
		name = "EventOpen",
		lockAtMostFor = "2m",
		lockAtLeastFor = "10s"
	)
	public void openTicketing() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		int processed = 0;
		int failed = 0;

		try {
			// 시작로그
			MdcContext.putRunId(runId);
			log.info("SCHED_START job=EventOpen");

			LocalDateTime now = LocalDateTime.now();

			// QUEUE_READY 상태이면서 ticketOpenAt이 지난 이벤트 조회
			List<Event> events = eventRepository.findByStatus(EventStatus.QUEUE_READY);

			if (events.isEmpty()) {
				log.info("SCHED_END job=QueueEntry processed=0 failed=0 durationMs={}",
					System.currentTimeMillis() - startAt);
				return;
			}

			for (Event event : events) {
				try {
					MdcContext.putEventId(event.getId());

					// ticketOpenAt이 현재 시간보다 이전이거나 같으면 오픈
					if (event.getTicketOpenAt().isBefore(now)
						|| event.getTicketOpenAt().isEqual(now)) {

						// QUEUE_READY → OPEN 상태 변경
						event.changeStatus(EventStatus.OPEN);
						eventRepository.save(event);
						processed++;

						log.info(
							"SCHED_EVENT_SUCCESS job=EventOpen eventId={} status=OPEN",
							event.getId()
						);
					}
				} catch (Exception ex) {
					failed++;
					log.error(
						"SCHED_EVENT_FAIL job=EventOpen eventId={} error={}",
						event.getId(), ex.toString(), ex
					);
				} finally {
					MdcContext.removeEventId();
				}
			}

			// 종료 로그
			log.info(
				"SCHED_END job=EventOpen processed={} failed={} durationMs={}",
				processed,
				failed,
				System.currentTimeMillis() - startAt
			);
		} catch (Exception ex) {
			log.error(
				"SCHED_FAIL job=EventOpen durationMs={} error={}",
				System.currentTimeMillis() - startAt,
				ex.toString(),
				ex
			);
		} finally {
			MdcContext.removeRunId();
		}
	}
}
