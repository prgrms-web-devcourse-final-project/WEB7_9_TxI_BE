package com.back.api.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.api.queue.service.QueueEntryProcessService;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!dev") //임시 스케줄러 차단
public class QueueExpireScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryProcessService queueEntryProcessService;

	@Scheduled(cron = "${queue.scheduler.expire.cron}", zone = "Asia/Seoul")
	public void autoExpireEntries() {
		try {
			LocalDateTime now = LocalDateTime.now();

			List<QueueEntry> expiredEntries = queueEntryRepository.findExpiredEntries(
				QueueEntryStatus.ENTERED,
				now
			);

			if (expiredEntries.isEmpty()) {
				return;
			}

			log.info("만료 대상 : {}명", expiredEntries.size());

			queueEntryProcessService.expireBatchEntries(expiredEntries);

			log.info("만료 처리 완료 : {}명", expiredEntries.size());
		} catch (Exception e) {
			log.error("자동 만료 스케줄러 실패", e);
		}

	}

}
