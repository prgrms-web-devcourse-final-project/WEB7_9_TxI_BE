package com.back.api.queue.scheduler;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.event.service.EventService;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;

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
	public void autoQueueEntries() {
		try {
			List<Event> openEvents = eventService.findEventsByStatus((EventStatus.OPEN));

			if (openEvents.isEmpty()) {
				return;
			}

			for (Event event : openEvents) {
				queueEntryProcessService.processEventQueueEntries(event);
			}
		} catch (Exception e) {
			log.error("자동 입장 스케줄러 실패", e);
		}
	}

}
