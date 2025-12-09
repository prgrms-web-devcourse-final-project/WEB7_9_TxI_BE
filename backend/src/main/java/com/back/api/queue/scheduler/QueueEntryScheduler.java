package com.back.api.queue.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.queue.service.QueueEntryProcessService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.global.properties.QueueSchedulerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 입장 처리 스케줄러
 * 대기열 상위 100명씩 자동 입장
 * WAITING -> ENTERED
 * 스케줄러 동작 주기
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEntryScheduler {

	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final QueueEntryProcessService queueEntryProcessService;
	private final EventRepository eventRepository; //TODO service로 변경 필요
	private final QueueSchedulerProperties properties;

	//대기열 자동 입장 처리
	@Scheduled(cron = "${queue.scheduler.entry.cron}", zone = "Asia/Seoul") //10초마다 실행
	public void autoQueueEntries() {
		try {
			List<Event> openEvents = eventRepository.findByStatusIn(
				List.of(EventStatus.OPEN)
			);

			if (openEvents.isEmpty()) {
				log.debug("No open events found for queue entry processing");
				return;
			}

			for (Event event : openEvents) {
				processEventQueueEntries(event);
			}
		} catch (Exception e) {
			log.error("Auto queue entry scheduler failed", e);
		}
	}

	//특정 이벤트 대기열 처리
	private void processEventQueueEntries(Event event) {
		Long eventId = event.getId();

		Long totalWaitingCount = queueEntryRedisRepository.getTotalWaitingCount(eventId);

		if (totalWaitingCount == 0) {
			log.debug("No waiting users for eventId: {}", eventId);
			return;
		}

		int batchSize = properties.getEntry().getBatchSize();
		batchSize =  Math.min(batchSize, totalWaitingCount.intValue());

		Set<Object> topWaitingUsers = queueEntryRedisRepository.getTopWaitingUsers(eventId, batchSize);

		if (topWaitingUsers.isEmpty()) {
			log.debug("No top waiting users found for eventId: {}", eventId);
			return;
		}

		List<Long> userIds = new ArrayList<>();

		for (Object userId : topWaitingUsers) {
			userIds.add(Long.parseLong(userId.toString()));
		}

		queueEntryProcessService.processBatchEntry(eventId, userIds);

	}


}
