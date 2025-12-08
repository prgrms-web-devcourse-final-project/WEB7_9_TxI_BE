package com.back.api.queue.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 처리 로직
 * QueueEntry 입장 처리
 * 스케줄러, 배치 활용
 * 대기중 -> 입장 완료
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEntryProcessService {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	@Transactional
	public void processEntry(Long eventId, Long userId) {
		QueueEntry entry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		validateEntry(entry);

		entry.enterQueue();
		queueEntryRepository.save(entry);

		updateRedis(eventId, userId);
		//TODO 입장완료 알림 로직 구현
	}

	@Transactional
	public void processBatchEntry(Long eventId, List<Long> userIds) {

		int successCount = 0;
		int failCount = 0;

		for (Long userId : userIds) {
			try {
				processEntry(eventId, userId);
				successCount++;
			} catch (ErrorException e) {
				log.error("Failed to process entry for eventId {} and userId {}: {}", eventId, userId, e.getMessage());
				failCount++;
			}
		}
	}

	private void validateEntry(QueueEntry queueEntry) {
		QueueEntryStatus status = queueEntry.getQueueEntryStatus();

		//이미 입장한 경우
		if (status == QueueEntryStatus.ENTERED) {
			throw new ErrorException(QueueEntryErrorCode.ALREADY_ENTERED);
		}

		//이미 만료된 경우
		if (status == QueueEntryStatus.EXPIRED) {
			throw new ErrorException(QueueEntryErrorCode.ALREADY_EXPIRED);
		}

		//대기중 상태가 아닌 경우
		if (status != QueueEntryStatus.WAITING) {
			throw new ErrorException(QueueEntryErrorCode.NOT_WAITING_STATUS);
		}
	}

	private void updateRedis(Long eventId, Long userId) {
		try {
			queueEntryRedisRepository.moveToEnteredQueue(eventId, userId);
			queueEntryRedisRepository.incrementEnteredCount(eventId);
			log.debug("Success to update eventId {} to Redis", eventId);

		} catch (Exception e) {
			log.error("Failed to update eventId {} to Redis", eventId);
		}
	}

	public boolean canEnterEntry(Long eventId, Long userId) {
		return queueEntryRepository
			.findByEvent_IdAndUser_Id(eventId, userId)
			.map(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.WAITING)
			.orElse(false);
	}

	@Transactional
	public void expireEntry(Long eventId, Long userId) {
		QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		if (queueEntry.getQueueEntryStatus() == QueueEntryStatus.EXPIRED) {
			log.debug("Already expired queue entry cannot be expired again.");
			return;
		}

		if (queueEntry.getQueueEntryStatus() != QueueEntryStatus.ENTERED) {
			log.debug("Only entered queue entry can be expired.");
			return;
		}

		queueEntry.expire();
		queueEntryRepository.save(queueEntry);

		try {
			queueEntryRedisRepository.removeFromEnteredQueue(eventId, userId);
			log.debug("Success to Expire eventId {} to Redis", eventId);
		} catch (Exception e) {
			log.error("Failed to Expire eventId {} to Redis", eventId);
		}

		//TODO 알림 로직 구현 필요
	}

	@Transactional
	public void expireBatchEntries(List<QueueEntry> entries) {

		int successCount = 0;

		for (QueueEntry entry : entries) {
			expireEntry(entry.getEventId(), entry.getUserId());
			successCount++;
		}

		log.info("Success to Expire {} queue entries in batch.", successCount);
	}

}
