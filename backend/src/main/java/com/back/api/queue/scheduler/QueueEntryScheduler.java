package com.back.api.queue.scheduler;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.back.api.event.service.EventService;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.global.observability.MdcContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 입장 처리 스케줄러
 * WAITING -> ENTERED
 * WAITING 상태 사용자에게 실시간 순위 업데이트 (WebSocket)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class QueueEntryScheduler {

	private final QueueEntryProcessService queueEntryProcessService;
	private final EventService eventService;

	//대기열 자동 입장 처리
	@Scheduled(cron = "${queue.scheduler.entry.cron}", zone = "Asia/Seoul") //10초마다 실행
	@SchedulerLock(
		name = "QueueEntry",
		lockAtMostFor = "2m",
		lockAtLeastFor = "5s"
	)
	public void autoQueueEntries() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		int processedEvents = 0;
		int failedEvents = 0;

		try {
			// 시작로그
			MdcContext.putRunId(runId);
			log.info("SCHED_START job=QueueEntry");

			List<Event> openEvents = eventService.findEventsByStatus((EventStatus.OPEN));

			if (openEvents.isEmpty()) {
				log.info("SCHED_END job=QueueEntry processed=0 failed=0 durationMs={}",
					System.currentTimeMillis() - startAt);
				return;
			}

			for (Event event : openEvents) {
				try {
					MdcContext.putEventId(event.getId());
					queueEntryProcessService.processEventQueueEntries(event);
					processedEvents++;
				} catch (Exception ex) {
					failedEvents++;
					// 실패 로그
					log.error("SCHED_EVENT_FAIL job=QueueEntry eventId={} error={}", event.getId(), ex.toString(), ex);
				} finally {
					MdcContext.removeEventId();
				}
			}

			// 종료 로그
			log.info(
				"SCHED_END job=QueueEntry processed={} failed={} durationMs={}",
				processedEvents,
				failedEvents,
				System.currentTimeMillis() - startAt
			);
		} catch (Exception ex) {
			log.error(
				"SCHED_FAIL job=QueueEntry durationMs={} error={}",
				System.currentTimeMillis() - startAt,
				ex.toString(),
				ex
			);
		} finally {
			MdcContext.removeRunId();
		}
	}

}
