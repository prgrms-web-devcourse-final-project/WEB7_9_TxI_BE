package com.back.api.event.scheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.event.EventScheduleEvent;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.observability.MdcContext;
import com.back.global.observability.metrics.SchedulerMetrics;
import com.back.global.scheduler.SchedulerLockHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"perf", "dev", "prod"})
public class EventScheduler {

	@Qualifier("dynamicScheduler")
	private final ThreadPoolTaskScheduler dynamicScheduler;

	private final EventRepository eventRepository;
	private final SchedulerMetrics schedulerMetrics;
	private final SchedulerLockHelper lockHelper;

	private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks
		= new ConcurrentHashMap<>();

	@EventListener
	@Transactional(readOnly = true)
	public void handleEventScheduleEvent(EventScheduleEvent event) {
		switch (event.getType()) {
			case CREATED -> scheduleEventLifecycle(event.getEventId());
			case UPDATED -> rescheduleEventLifecycle(event.getEventId());
			case DELETED -> cancelEventSchedules(event.getEventId());
		}
	}

	private void scheduleEventLifecycle(Long eventId) {
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

		LocalDateTime now = LocalDateTime.now();

		int scheduledCount = 0;

		// 1. PRE_OPEN 스케줄 (READY → PRE_OPEN)
		if (event.getPreOpenAt().isAfter(now)) {
			scheduleTask(
				eventId,
				"PRE_OPEN",
				event.getPreOpenAt(),
				() -> transitionEventStatus(eventId, EventStatus.PRE_OPEN)
			);
			scheduledCount++;
		}

		// 2. PRE_CLOSE 스케줄 (PRE_OPEN → PRE_CLOSED)
		if (event.getPreCloseAt().isAfter(now)) {
			scheduleTask(
				eventId,
				"PRE_CLOSE",
				event.getPreCloseAt(),
				() -> transitionEventStatus(eventId, EventStatus.PRE_CLOSED)
			);
			scheduledCount++;
		}

		// 3. TICKET_OPEN 스케줄 (QUEUE_READY → OPEN)
		if (event.getTicketOpenAt().isAfter(now)) {
			scheduleTask(
				eventId,
				"TICKET_OPEN",
				event.getTicketOpenAt(),
				() -> transitionEventStatus(eventId, EventStatus.OPEN)
			);
			scheduledCount++;
		}

		// 4. TICKET_CLOSE 스케줄 (OPEN → CLOSED)
		if (event.getTicketCloseAt().isAfter(now)) {
			scheduleTask(
				eventId,
				"TICKET_CLOSE",
				event.getTicketCloseAt(),
				() -> transitionEventStatus(eventId, EventStatus.CLOSED)
			);
			scheduledCount++;
		}

		log.info("EVENT_LIFECYCLE_SCHEDULED eventId={} tasks={}", eventId, scheduledCount);
	}

	// 이벤트 수정 시 스케줄러 취소 후 재등록
	private void rescheduleEventLifecycle(Long eventId) {
		cancelEventSchedules(eventId);
		scheduleEventLifecycle(eventId);
	}

	// 이벤트 삭제 시 스케줄러 취소
	private void cancelEventSchedules(Long eventId) {
		String prefix = eventId + "-";

		var iterator = scheduledTasks.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			if (entry.getKey().startsWith(prefix)) {
				entry.getValue().cancel(false);
				iterator.remove();
			}
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional(readOnly = true)
	public void recoverSchedulesOnStartup() {

		LocalDateTime now = LocalDateTime.now();
		List<Event> upcomingEvents = eventRepository.findUpcomingEvents(now);

		int successCount = 0;
		int failCount = 0;

		for (Event event : upcomingEvents) {
			try {
				scheduleEventLifecycle(event.getId());
				successCount++;
			} catch (Exception e) {
				failCount++;
			}
		}

		log.info("EVENT_LIFECYCLE_RECOVERY_COMPLETE success={} failed={}",
			successCount, failCount);
	}

	// 작업 스케줄 등록
	private void scheduleTask(
		Long eventId,
		String taskType,
		LocalDateTime executeAt,
		Runnable task
	) {
		String taskKey = eventId + "-" + taskType;

		// 기존 작업이 있으면 취소
		ScheduledFuture<?> existing = scheduledTasks.get(taskKey);
		if (existing != null && !existing.isDone()) {
			existing.cancel(false);
		}

		// 서울 시간대로 변환
		Instant instant = executeAt
			.atZone(ZoneId.of("Asia/Seoul"))
			.toInstant();

		// 스케줄 등록
		ScheduledFuture<?> future = dynamicScheduler.schedule(
			wrapWithMonitoring(taskKey, task),
			instant
		);

		scheduledTasks.put(taskKey, future);
	}

	// 이벤트 상태 전환
	private void transitionEventStatus(Long eventId, EventStatus toStatus) {
		String lockName = "EventTransition-" + eventId + "-" + toStatus;

		// 분산 락을 획득하고 실행
		boolean executed = lockHelper.executeWithLock(lockName, () -> {
			MdcContext.putEventId(eventId);
			try {
				Event event = eventRepository.findById(eventId)
					.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

				event.changeStatus(toStatus);
				eventRepository.save(event);

			} finally {
				MdcContext.removeEventId();
			}
		});

		if (!executed) {
			log.warn("EVENT_TRANSITION_SKIPPED_LOCKED eventId={} status={}",
				eventId, toStatus);
		}
	}

	private Runnable wrapWithMonitoring(String taskKey, Runnable task) {
		return () -> {
			String runId = UUID.randomUUID().toString();
			long startAt = System.currentTimeMillis();

			try {
				MdcContext.putRunId(runId);

				// 실제 작업 실행
				task.run();

				long duration = System.currentTimeMillis() - startAt;
				schedulerMetrics.recordDuration("Dynamic-" + taskKey, duration);

				log.info("SCHED_DYNAMIC_SUCCESS task={} durationMs={}",
					taskKey, duration);

			} catch (Exception e) {
				long duration = System.currentTimeMillis() - startAt;
				schedulerMetrics.recordDuration("Dynamic-" + taskKey, duration);

				log.error("SCHED_DYNAMIC_FAIL task={} durationMs={} error={}",
					taskKey, duration, e.getMessage(), e);

			} finally {
				scheduledTasks.remove(taskKey);
				MdcContext.removeRunId();
			}
		};
	}
}
