package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.back.api.queue.service.QueueEntryProcessService;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.observability.MdcContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"perf"})
public class QueueExpireScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryProcessService queueEntryProcessService;

	@Scheduled(cron = "${queue.scheduler.expire.cron}", zone = "Asia/Seoul")
	@SchedulerLock(
		name = "QueueExpire",
		lockAtMostFor = "5m",
		lockAtLeastFor = "10s"
	)
	public void autoExpireEntries() {
		String runId = UUID.randomUUID().toString();
		long startAt = System.currentTimeMillis();

		int processed = 0;
		int failed = 0;

		try {
			MdcContext.putRunId(runId);
			log.info("SCHED_START job=QueueExpire");

			LocalDateTime now = LocalDateTime.now();

			List<QueueEntry> expiredEntries = queueEntryRepository.findExpiredEntries(
				QueueEntryStatus.ENTERED,
				now
			);

			if (expiredEntries.isEmpty()) {
				log.info(
					"SCHED_END job=QueueExpire processed=0 failed=0 durationMs={}",
					System.currentTimeMillis() - startAt
				);
				return;
			}

			log.info(
				"SCHED_BATCH_FOUND job=QueueExpire candidates={}",
				expiredEntries.size()
			);

			try {
				queueEntryProcessService.expireBatchEntries(expiredEntries);
				processed = expiredEntries.size();
			} catch (Exception ex) {
				failed = expiredEntries.size();
				log.error("SCHED_BATCH_FAIL job=QueueExpire error={}", ex.toString(), ex);
			}

			log.info(
				"SCHED_END job=QueueExpire processed={} failed={} durationMs={}",
				processed,
				failed,
				System.currentTimeMillis() - startAt
			);
		} catch (Exception ex) {
			log.error(
				"SCHED_FAIL job=QueueExpire durationMs={} error={}",
				System.currentTimeMillis() - startAt,
				ex.toString(),
				ex
			);
		} finally {
			MdcContext.removeRunId();
		}

	}

}
