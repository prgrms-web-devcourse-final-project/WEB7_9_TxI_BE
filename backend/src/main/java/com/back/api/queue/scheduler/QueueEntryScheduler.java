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
//@Profile("!dev") //임시 스케줄러 차단
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
				return;
			}

			for (Event event : openEvents) {
				processEventQueueEntries(event);
			}
		} catch (Exception e) {
			log.error("자동 입장 스케줄러 실패", e);
		}
	}

	//특정 이벤트 대기열 처리
	private void processEventQueueEntries(Event event) {

		Long eventId = event.getId();

		//대기 중인 인원 확인
		Long totalWaitingCount = queueEntryRedisRepository.getTotalWaitingCount(eventId);

		if (totalWaitingCount == 0) {
			return;
		}

		//입장 완료된 인원 확인
		Long currentEnteredCount = queueEntryRedisRepository.getTotalEnteredCount(eventId);
		int maxEnteredLimit = properties.getEntry().getMaxEnteredLimit();

		//입장 가능한 인원 확인
		int availableEnteredCount = maxEnteredLimit - currentEnteredCount.intValue();

		if (availableEnteredCount < 0) {
			return;
		}

		//한번에 입장시킬 인원. 현재는 100명
		int batchSize = properties.getEntry().getBatchSize();

		// 입장 인원 선정 -> batchSize와 빈 자리 중 작은 값으로 설정
		// 빈 자리 순차적으로 들어갈 수 있도록 함
		int entryCount = Math.min(Math.min(batchSize, availableEnteredCount), totalWaitingCount.intValue());

		//상위 N명 추출
		Set<Object> topWaitingUsers = queueEntryRedisRepository.getTopWaitingUsers(eventId, entryCount);

		if (topWaitingUsers.isEmpty()) {
			return;
		}

		List<Long> userIds = new ArrayList<>();
		for (Object userId : topWaitingUsers) {
			userIds.add(Long.parseLong(userId.toString()));
		}

		queueEntryProcessService.processBatchEntry(eventId, userIds);

	}

}
