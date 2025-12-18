package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.event.service.EventService;
import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.properties.QueueSchedulerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 셔플 스케줄러
 * ticketOpenAt 1시간 전 대기열 셔플 자동 실행 -> 이벤트 상태 PREOPEN에서 READY로 변경
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class QueueShuffleScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueShuffleService queueShuffleService;
	private final EventService eventService;
	private final PreRegisterRepository preRegisterRepository; //TODO service로 변경 필요
	private final QueueSchedulerProperties properties;

	@Scheduled(cron = "${queue.scheduler.shuffle.cron}", zone = "Asia/Seoul")
	public void autoShuffleQueue() {
		try {
			LocalDateTime now = LocalDateTime.now();

			int timeRangeMinutes = properties.getShuffle().getTimeRangeMinutes();

			//오차 허용을 위해 앞뒤로 1분까지 검사
			LocalDateTime targetTime = now.plusHours(1);
			LocalDateTime rangeStart = targetTime.minusMinutes(timeRangeMinutes);
			LocalDateTime rangeEnd = targetTime.plusMinutes(timeRangeMinutes);

			List<Event> eventList = eventService.findEventsByTicketOpenAtBetweenAndStatus(
				rangeStart,
				rangeEnd,
				EventStatus.PRE_OPEN
			);

			if (eventList.isEmpty()) {
				return;
			}

			for (Event event : eventList) {
				shuffleQueueForEvent(event);
			}

		} catch (Exception e) {
			log.error("자동 셔플 스케줄러 실패", e);
		}
	}

	//특정 이벤트 셔플 처리
	private void shuffleQueueForEvent(Event event) {
		Long eventId = event.getId();

		long existingCount = queueEntryRepository.countByEvent_Id(eventId);

		if (existingCount > 0) {
			return;
		}

		//TODO PreRegisterService로 변경 필요
		List<Long> preRegisteredUserIds = preRegisterRepository.findRegisteredUserIdsByEventId(eventId);

		if (preRegisteredUserIds.isEmpty()) {
			log.info("이벤트 ID {}에 대한 사전 등록 사용자가 없습니다.", eventId);
			return;
		}

		queueShuffleService.shuffleQueue(eventId, preRegisteredUserIds);

	}
}
