package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 셔플 스케줄러
 * ticketOpenAt 1시간 전 대기열 셔플 자동 실행
 * 스케줄러 동작 주기 : 10분
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!dev") //임시 스케줄러 차단
public class QueueShuffleScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final EventRepository eventRepository; //TODO service로 변경 필요
	private final QueueShuffleService queueShuffleService;

	@Scheduled(cron = "0 */10 * * * *",  zone = "Asia/Seoul") //10분마다 실행
	public void autoShuffleQueue() {
		try {
			LocalDateTime now = LocalDateTime.now();

			LocalDateTime targetTime = now.plusHours(1);
			LocalDateTime rangeStart = targetTime.minusMinutes(10);
			LocalDateTime rangeEnd = targetTime.plusMinutes(10);

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
		//List<Long> preRegisteredUserIds = preRegisterService.getPreRegisteredUserIds(eventId);

		//임시 목 데이터
		List<Long> preRegisteredUserIds = getMockPreRegisteredUser(eventId);

		if (preRegisteredUserIds.isEmpty()) {
			log.info("No pre-registered users for eventId: {}", eventId);
			return;
		}

		queueShuffleService.shuffleQueue(eventId, preRegisteredUserIds);

	}

	//TODO 임시 목 데이터 삭제. 이후 PreRegisterService에서 조회
	private List<Long> getMockPreRegisteredUser(Long eventId) {
		return List.of(1L, 2L, 3L, 4L, 5L);
	}
}
