package com.back.api.queue.scheduler;

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
import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.observability.MdcContext;
import com.back.global.observability.metrics.SchedulerMetrics;
import com.back.global.scheduler.SchedulerLockHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 셔플 스케줄러
 * ticketOpenAt 1시간 전 대기열 셔플 자동 실행 -> 이벤트 상태 PRE_CLOSED에서 QUEUE_READY로 변경
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class QueueShuffleScheduler {

	@Qualifier("dynamicScheduler")
	private final ThreadPoolTaskScheduler dynamicScheduler;

	private final QueueEntryRepository queueEntryRepository;
	private final QueueShuffleService queueShuffleService;
	private final EventRepository eventRepository;
	private final PreRegisterRepository preRegisterRepository;
	private final SchedulerMetrics schedulerMetrics;
	private final SchedulerLockHelper lockHelper;

	private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


	@EventListener
	@Transactional(readOnly = true)
	public void handleEventScheduleEvent(EventScheduleEvent event) {
		switch (event.getType()) {
			case CREATED -> scheduleQueueShuffle(event.getEventId());
			case UPDATED -> rescheduleQueueShuffle(event.getEventId());
			case DELETED -> cancelQueueShuffle(event.getEventId());
		}
	}

	// 이벤트 생성 시 셔플 스케줄링
	private void scheduleQueueShuffle(Long eventId) {

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime shuffleTime = event.getTicketOpenAt().minusHours(1);

		if (!shuffleTime.isAfter(now)) {
			return;
		}

		String taskKey = eventId + "-SHUFFLE";

		// 기존 스케줄 취소
		ScheduledFuture<?> existing = scheduledTasks.get(taskKey);
		if (existing != null && !existing.isDone()) {
			existing.cancel(false);
		}

		// 스케줄 등록
		Instant instant = shuffleTime
			.atZone(ZoneId.of("Asia/Seoul"))
			.toInstant();

		ScheduledFuture<?> future = dynamicScheduler.schedule(
			wrapWithMonitoring(taskKey, () -> shuffleQueueForEvent(eventId)),
			instant
		);

		scheduledTasks.put(taskKey, future);

		log.info("SHUFFLE_SCHEDULED eventId={} executeAt={}", eventId, shuffleTime);
	}

	// 이벤트 수정 시 셔플 스케줄 수정
	private void rescheduleQueueShuffle(Long eventId) {
		cancelQueueShuffle(eventId);
		scheduleQueueShuffle(eventId);
	}

	// 이벤트 삭제 시 셔플 스케줄 취소
	private void cancelQueueShuffle(Long eventId) {
		String taskKey = eventId + "-SHUFFLE";
		ScheduledFuture<?> future = scheduledTasks.remove(taskKey);

		if (future != null) {
			future.cancel(false);
		}
	}

	// 서버 재시작 시 스케줄 복구
	@EventListener(ApplicationReadyEvent.class)
	@Transactional(readOnly = true)
	public void recoverSchedulesOnStartup() {

		LocalDateTime now = LocalDateTime.now();
		List<Event> upcomingEvents = eventRepository.findUpcomingEvents(now);

		int successCount = 0;

		for (Event event : upcomingEvents) {
			LocalDateTime shuffleTime = event.getTicketOpenAt().minusHours(1);

			if (!shuffleTime.isAfter(now)) {
				continue;
			}

			try {
				scheduleQueueShuffle(event.getId());
				successCount++;
			} catch (Exception e) {
				log.error("SHUFFLE_RECOVERY_FAIL eventId={}", event.getId(), e);
			}
		}

		log.info("SHUFFLE_RECOVERY_COMPLETE count={} total={}", successCount, scheduledTasks.size());
	}


	// 특정 이벤트 셔플 처리
	private void shuffleQueueForEvent(Long eventId) {
		String lockName = "QueueShuffle-" + eventId;

		boolean executed = lockHelper.executeWithLock(lockName, () -> {
			MdcContext.putEventId(eventId);
			try {
				// 이미 셔플되었는지 확인
				long existingCount = queueEntryRepository.countByEvent_Id(eventId);
				if (existingCount > 0) {
					log.info("SHUFFLE_SKIP_ALREADY_DONE eventId={}", eventId);
					return;
				}

				// 사전등록 사용자 조회
				List<Long> preRegisteredUserIds = preRegisterRepository.findRegisteredUserIdsByEventId(eventId);

				if (preRegisteredUserIds.isEmpty()) {
					log.warn("SHUFFLE_NO_USERS eventId={}", eventId);
					return;
				}

				// 셔플 실행
				queueShuffleService.shuffleQueue(eventId, preRegisteredUserIds);

				log.info("SHUFFLE_SUCCESS eventId={} users={}", eventId, preRegisteredUserIds.size());

			} finally {
				MdcContext.removeEventId();
			}
		});

		if (!executed) {
			log.warn("SHUFFLE_LOCKED eventId={}", eventId);
		}
	}

	private Runnable wrapWithMonitoring(String taskKey, Runnable task) {
		return () -> {
			String runId = UUID.randomUUID().toString();
			long startAt = System.currentTimeMillis();

			try {
				MdcContext.putRunId(runId);
				task.run();

				long duration = System.currentTimeMillis() - startAt;
				schedulerMetrics.recordDuration("Shuffle-" + taskKey, duration);

			} catch (Exception e) {
				long duration = System.currentTimeMillis() - startAt;
				schedulerMetrics.recordDuration("Shuffle-" + taskKey, duration);

				log.error("SHUFFLE_FAIL task={} durationMs={} error={}",
					taskKey, duration, e.getMessage(), e);

			} finally {
				scheduledTasks.remove(taskKey);
				MdcContext.removeRunId();
			}
		};
	}
}
