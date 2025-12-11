package com.back.api.event.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!dev") //임시 스케줄러 차단
public class EventOpenScheduler {

	private final EventRepository eventRepository;

	@Scheduled(cron = "${event.scheduler.open.cron}", zone = "Asia/Seoul") // 매 분 실행
	public void openTicketing() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// QUEUE_READY 상태이면서 ticketOpenAt이 지난 이벤트 조회
			List<Event> events = eventRepository.findByStatus(EventStatus.QUEUE_READY);

			if (events.isEmpty()) {
				return;
			}

			for (Event event : events) {
				// ticketOpenAt이 현재 시간보다 이전이거나 같으면 오픈
				if (event.getTicketOpenAt().isBefore(now)
					|| event.getTicketOpenAt().isEqual(now)) {


					// QUEUE_READY → OPEN 상태 변경
					event.changeStatus(EventStatus.OPEN);
					eventRepository.save(event);

					log.info("이벤트 상태 변경: QUEUE_READY → OPEN");
				}
			}

		} catch (Exception e) {
			log.error("티켓팅 오픈 스케줄러 실행 실패", e);
		}
	}
}
