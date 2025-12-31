package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.back.api.event.service.EventService;
import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.logging.MdcContext;
import com.back.global.properties.QueueSchedulerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 셔플 스케줄러
 * ticketOpenAt 1시간 전 대기열 셔플 자동 실행 -> 이벤트 상태 PRE_CLOSED에서 QUEUE_READY로 변경
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class QueueShuffleScheduler {
	private static final String JOB_NAME = "QueueShuffle";

	private final QueueEntryRepository queueEntryRepository;
	private final QueueShuffleService queueShuffleService;
	private final EventService eventService;
	private final PreRegisterRepository preRegisterRepository;
	private final QueueSchedulerProperties properties;

	@Scheduled(cron = "${queue.scheduler.shuffle.cron}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "QueueShuffle",
		lockAtMostFor = "10m",
		lockAtLeastFor = "30s"
	)
	public void autoShuffleQueue() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		int processed = 0;
		int skipped = 0;
		int failed = 0;

		try {
			MdcContext.putRunId(runId);
			log.info("SCHED_START job={}", JOB_NAME);

			LocalDateTime now = LocalDateTime.now();
			int timeRangeMinutes = properties.getShuffle().getTimeRangeMinutes();

			//오차 허용을 위해 앞뒤로 1분까지 검사
			LocalDateTime targetTime = now.plusHours(1);
			LocalDateTime rangeStart = targetTime.minusMinutes(timeRangeMinutes);
			LocalDateTime rangeEnd = targetTime.plusMinutes(timeRangeMinutes);

			List<Event> eventList = eventService.findEventsByTicketOpenAtBetweenAndStatus(
				rangeStart,
				rangeEnd,
				EventStatus.PRE_CLOSED
			);

			if (eventList.isEmpty()) {
				log.info(
					"SCHED_END job={} processed=0 skipped=0 failed=0 durationMs={}",
					JOB_NAME,
					System.currentTimeMillis() - startAt
				);
				return;
			}
			log.info(
				"SCHED_BATCH_FOUND job={} candidates={}",
				JOB_NAME,
				eventList.size()
			);

			for (Event event : eventList) {
				Long eventId = event.getId();
				try {
					MdcContext.putEventId(eventId);

					boolean shuffled = shuffleQueueForEvent(event);
					if (shuffled) {
						processed++;
					} else {
						skipped++;
					}

				} catch (Exception ex) {
					failed++;
					log.error(
						"SCHED_ITEM_FAIL job={} eventId={} error={}",
						JOB_NAME,
						eventId,
						ex.toString(),
						ex
					);
				} finally {
					MdcContext.removeEventId();
				}
			}
			log.info(
				"SCHED_END job={} processed={} skipped={} failed={} durationMs={}",
				JOB_NAME,
				processed,
				skipped,
				failed,
				System.currentTimeMillis() - startAt
			);
		} catch (Exception ex) {
			log.error(
				"SCHED_FAIL job={} durationMs={} error={}",
				JOB_NAME,
				System.currentTimeMillis() - startAt,
				ex.toString(),
				ex
			);
		} finally {
			MdcContext.removeRunId();
		}
	}

	/**
	 * //특정 이벤트 셔플 처리
	 * @return true: 셔플 성공, false: skipped
	 */
	private boolean shuffleQueueForEvent(Event event) {
		Long eventId = event.getId();

		long existingCount = queueEntryRepository.countByEvent_Id(eventId);

		if (existingCount > 0) {
			log.info(
				"SCHED_ITEM_SKIP job={} eventId={} reason=ALREADY_SHUFFLED",
				JOB_NAME,
				eventId
			);
			return false;
		}

		List<Long> preRegisteredUserIds = preRegisterRepository.findRegisteredUserIdsByEventId(eventId);

		if (preRegisteredUserIds.isEmpty()) {
			log.info(
				"SCHED_ITEM_SKIP job={} eventId={} reason=NO_PRE_REGISTER",
				JOB_NAME,
				eventId
			);
			return false;
		}

		queueShuffleService.shuffleQueue(eventId, preRegisteredUserIds);
		log.info(
			"SCHED_ITEM_SUCCESS job={} eventId={} users={}",
			JOB_NAME,
			eventId,
			preRegisteredUserIds.size()
		);
		return true;
	}
}
