package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.properties.QueueSchedulerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 셔플 스케줄러
 * ticketOpenAt 1시간 전 대기열 셔플 자동 실행 -> 이벤트 상태 PREOPEN에서 READY로 변경
 * 스케줄러 동작 주기 : 10분
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueShuffleScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final EventRepository eventRepository; //TODO service로 변경 필요
	private final QueueShuffleService queueShuffleService;
	private final QueueSchedulerProperties properties;
	private final PreRegisterRepository preRegisterRepository; //TODO service로 변경 필요

	@Scheduled(cron = "${queue.scheduler.shuffle.cron}",  zone = "Asia/Seoul") //10분마다 실행
	public void autoShuffleQueue() {
		try {
			LocalDateTime now = LocalDateTime.now();

			int timeRangeMinutes = properties.getShuffle().getTimeRangeMinutes();

			LocalDateTime targetTime = now.plusHours(1);
			LocalDateTime rangeStart = targetTime.minusMinutes(timeRangeMinutes);
			LocalDateTime rangeEnd = targetTime.plusMinutes(timeRangeMinutes);

			List<Event> eventList = eventRepository.findByTicketOpenAtBetweenAndStatus(
				rangeStart,
				rangeEnd,
				EventStatus.PRE_OPEN
			);

			if (eventList.isEmpty()) {
				log.info("No events found for auto shuffle");
				return;
			}

			log.info("Found {} events for auto shuffle", eventList.size());

			for (Event event : eventList) {
				shuffleQueueForEvent(event);
			}

		} catch (Exception e) {
			log.error("Auto shuffle scheduler failed", e);
		}
	}

	//특정 이벤트 셔플 처리
	private void shuffleQueueForEvent(Event event) {
		Long eventId = event.getId();

		long existingCount = queueEntryRepository.countByEvent_Id(eventId);

		if (existingCount > 0) {
			log.info("Queue already shuffled for eventId: {}", eventId);
			return;
		}

		//TODO PreRegisterService로 변경 필요
		List<Long> preRegisteredUserIds = preRegisterRepository.findRegisteredUserIdsByEventId(eventId);

		if (preRegisteredUserIds.isEmpty()) {
			log.info("No pre-registered users for eventId: {}", eventId);
			return;
		}

		queueShuffleService.shuffleQueue(eventId, preRegisteredUserIds);

	}
}
