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
@Profile({"perf", "dev"})
public class EventOpenScheduler {

	private final EventRepository eventRepository;

	/**
	 * READY → PRE_OPEN (사전등록 시작)
	 * preOpenAt 시간이 되면 사전등록 오픈
	 */
	@Scheduled(cron = "${event.scheduler.status.cron:0 * * * * *}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "EventPreOpen",
		lockAtMostFor = "2m",
		lockAtLeastFor = "10s"
	)
	public void openPreRegistration() {
		processStatusTransition(
			EventStatus.READY,
			EventStatus.PRE_OPEN,
			"PreOpen",
			(event, now) -> !event.getPreOpenAt().isAfter(now)
		);
	}

	/**
	 * PRE_OPEN → PRE_CLOSED (사전등록 마감)
	 * preCloseAt 시간이 되면 사전등록 마감
	 */
	@Scheduled(cron = "${event.scheduler.status.cron:0 * * * * *}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "EventPreClose",
		lockAtMostFor = "2m",
		lockAtLeastFor = "10s"
	)
	public void closePreRegistration() {
		processStatusTransition(
			EventStatus.PRE_OPEN,
			EventStatus.PRE_CLOSED,
			"PreClose",
			(event, now) -> !event.getPreCloseAt().isAfter(now)
		);
	}

	/**
	 * QUEUE_READY → OPEN (티켓팅 시작)
	 * ticketOpenAt 시간이 되면 티켓팅 오픈
	 *
	 * 참고: PRE_CLOSED → QUEUE_READY 전환은 QueueShuffleScheduler에서 처리
	 * (ticketOpenAt 1시간 전에 랜덤 큐 생성 후 QUEUE_READY로 상태 변경)
	 */
	@Scheduled(cron = "${event.scheduler.open.cron}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "EventOpen",
		lockAtMostFor = "2m",
		lockAtLeastFor = "10s"
	)
	public void openTicketing() {
		processStatusTransition(
			EventStatus.QUEUE_READY,
			EventStatus.OPEN,
			"EventOpen",
			(event, now) -> !event.getTicketOpenAt().isAfter(now)
		);
	}

	/**
	 * OPEN → CLOSED (티켓팅 마감)
	 * ticketCloseAt 시간이 되면 티켓팅 마감
	 */
	@Scheduled(cron = "${event.scheduler.status.cron:0 * * * * *}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "EventClose",
		lockAtMostFor = "2m",
		lockAtLeastFor = "10s"
	)
	public void closeTicketing() {
		processStatusTransition(
			EventStatus.OPEN,
			EventStatus.CLOSED,
			"EventClose",
			(event, now) -> !event.getTicketCloseAt().isAfter(now)
		);
	}

	/**
	 * 이벤트 상태 전환 공통 로직
	 */
	private void processStatusTransition(
		EventStatus fromStatus,
		EventStatus toStatus,
		String jobName,
		StatusTransitionCondition condition
	) {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		int processed = 0;
		int failed = 0;

		try {
			MdcContext.putRunId(runId);
			log.info("SCHED_START job={}", jobName);

			LocalDateTime now = LocalDateTime.now();

			// fromStatus 상태인 이벤트 조회
			List<Event> events = eventRepository.findByStatus(fromStatus);

			if (events.isEmpty()) {
				log.info("SCHED_END job={} processed=0 failed=0 durationMs={}",
					jobName, System.currentTimeMillis() - startAt);
				return;
			}

			for (Event event : events) {
				try {
					MdcContext.putEventId(event.getId());

					// 조건 확인
					if (condition.shouldTransition(event, now)) {
						// 상태 변경
						event.changeStatus(toStatus);
						eventRepository.save(event);
						processed++;

						log.info(
							"SCHED_EVENT_SUCCESS job={} eventId={} status={} -> {}",
							jobName, event.getId(), fromStatus, toStatus
						);
					}
				} catch (Exception ex) {
					failed++;
					log.error(
						"SCHED_EVENT_FAIL job={} eventId={} error={}",
						jobName, event.getId(), ex.toString(), ex
					);
				} finally {
					MdcContext.removeEventId();
				}
			}

			log.info(
				"SCHED_END job={} processed={} failed={} durationMs={}",
				jobName, processed, failed, System.currentTimeMillis() - startAt
			);
		} catch (Exception ex) {
			log.error(
				"SCHED_FAIL job={} durationMs={} error={}",
				jobName, System.currentTimeMillis() - startAt, ex.toString(), ex
			);
		} finally {
			MdcContext.removeRunId();
		}
	}

	/**
	 * 상태 전환 조건을 정의하는 함수형 인터페이스
	 */
	@FunctionalInterface
	private interface StatusTransitionCondition {
		boolean shouldTransition(Event event, LocalDateTime now);
	}
}
