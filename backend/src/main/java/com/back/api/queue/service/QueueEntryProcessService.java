package com.back.api.queue.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.EnteredQueueResponse;
import com.back.api.queue.dto.response.ExpiredQueueResponse;
import com.back.api.queue.dto.response.WaitingQueueBatchEventResponse;
import com.back.api.queue.dto.response.WaitingQueueResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.event.EventPublisher;
import com.back.global.properties.QueueSchedulerProperties;

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
	private final EventPublisher eventPublisher;
	private final QueueSchedulerProperties properties;
	private final QueueEntryReadService queueEntryReadService;


	/* ==================== 입장 처리 ==================== */

	@Transactional
	public void processEntry(Long eventId, Long userId) {
		QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		validateEntry(queueEntry);

		queueEntry.enterQueue();
		queueEntryRepository.save(queueEntry);

		updateRedis(eventId, userId);

		publishEnteredEvent(queueEntry); // 입장 처리 웹소켓 이벤트 발행

		//TODO 입장완료 알림 로직 구현
	}

	@Transactional
	public void processBatchEntry(Long eventId, List<Long> userIds) {

		for (Long userId : userIds) {
			try {
				processEntry(eventId, userId);
			} catch (ErrorException e) {
				log.error("eventId {} / userId {} 처리 중 오류 발생: {}", eventId, userId, e.getMessage());
			}
		}
	}

	/* ==================== 이벤트 단위 자동 입장 처리 (스케줄러) ==================== */

	@Transactional
	public void processEventQueueEntries(Event event) {

		Long eventId = event.getId();

		//대기 중인 인원 확인
		Long totalWaitingCount = queueEntryRedisRepository.getTotalWaitingCount(eventId);

		if (totalWaitingCount == 0) {
			return;
		}

		//입장 완료된 인원 확인
		Long currentEnteredCount = queueEntryRedisRepository.getTotalEnteredCount(eventId);
		int maxEnteredLimit = properties.getEntry().getMaxEnteredLimit();

		//입장 가능한 인원 확인
		int availableEnteredCount = maxEnteredLimit - currentEnteredCount.intValue();

		if (availableEnteredCount <= 0) {
			log.info("[EventId: {}] 최대 수용 인원 도달 - 현재: {}명, 최대: {}명",
				eventId, currentEnteredCount, maxEnteredLimit);
			return;
		}

		//한번에 입장시킬 인원
		int batchSize = properties.getEntry().getBatchSize();

		// 입장 인원 선정
		// 빈 자리 순차적으로 들어갈 수 있도록 함
		int entryCount = Math.min(
			batchSize,
			Math.min(availableEnteredCount, totalWaitingCount.intValue())  // 빈 자리와 대기 인원 중 작은 값
		);

		log.info("입장 처리 - eventId: {}, 대기: {}명, 입장완료: {}명, 빈자리: {}명, 배치사이즈: {}명, 입장시킬인원: {}명",
			eventId, totalWaitingCount, currentEnteredCount, availableEnteredCount, batchSize, entryCount);


		//상위 N명 추출
		Set<Object> topWaitingUsers = queueEntryRedisRepository.getTopWaitingUsers(eventId, entryCount);

		if (topWaitingUsers.isEmpty()) {
			return;
		}

		List<Long> userIds = new ArrayList<>();
		for (Object userId : topWaitingUsers) {
			userIds.add(Long.parseLong(userId.toString()));
		}

		processBatchEntry(eventId, userIds); // 입장 순서인 사용자 입장처리

		publishWaitingUpdateEvents(eventId); // 대기중인 사용자 실시간 순위 업데이트

	}

	public boolean canEnterEntry(Long eventId, Long userId) {
		return queueEntryRepository
			.findByEvent_IdAndUser_Id(eventId, userId)
			.map(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.WAITING)
			.orElse(false);
	}

	/* ==================== 만료 처리 ==================== */

	@Transactional
	public void expireEntry(Long eventId, Long userId) {
		QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		if (queueEntry.getQueueEntryStatus() == QueueEntryStatus.EXPIRED) {
			return;
		}

		if (queueEntry.getQueueEntryStatus() != QueueEntryStatus.ENTERED) {
			return;
		}

		queueEntry.expire();
		queueEntryRepository.save(queueEntry);

		try {
			queueEntryRedisRepository.removeFromEnteredQueue(eventId, userId);
			log.debug("eventId {} - Redis 만료 처리 성공", eventId);
		} catch (Exception e) {
			log.error("eventId {} - Redis 만료 처리 실패", eventId);
		}

		publishExpiredEvent(queueEntry);  // 만료 처리 웹소켓 이벤트 발행

		//TODO 알림 로직 구현 필요
	}

	@Transactional
	public void expireBatchEntries(List<QueueEntry> entries) {

		int successCount = 0;

		for (QueueEntry entry : entries) {
			expireEntry(entry.getEventId(), entry.getUserId());
			successCount++;
		}
		log.info("총 {}개 대기열 항목 만료 처리 완료", successCount);
	}


	/* ==================== 결제 완료 처리 ==================== */

	//TODO 결제 도메인에서 사용 필요
	@Transactional
	public void completePayment(Long eventId, Long userId) {

		QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		validatePaymentCompletion(queueEntry);

		queueEntry.completePayment();
		queueEntryRepository.save(queueEntry);

		try {
			queueEntryRedisRepository.removeFromEnteredQueue(eventId, userId);
		} catch (Exception e) {
			log.error("결제 완료 사용자 대기열 제거 실패");
		}

		publishCompletedEvent(queueEntry); // 결제 완료 처리 웹소켓 이벤트 발행

	}

	/* ==================== 대기열 실시간 업데이트 이벤트 ==================== */

	public void publishWaitingUpdateEvents(Long eventId) {
		try {

			Set<ZSetOperations.TypedTuple<Object>> allWaitingUsers =
				queueEntryRedisRepository.getAllWaitingUsersWithRank(eventId);

			if (allWaitingUsers == null || allWaitingUsers.isEmpty()) {
				return;
			}

			int totalWaitingCount = allWaitingUsers.size();

			Map<Long, WaitingQueueResponse> allUpdates = new HashMap<>();
			int rank = 1;

			for (ZSetOperations.TypedTuple<Object> tuple : allWaitingUsers) {
				try {
					Long userId = Long.parseLong(tuple.getValue().toString());
					int waitingAhead = rank - 1;

					WaitingQueueResponse response = queueEntryReadService
						.buildWaitingQueueResponseFromRank(
							userId,
							eventId,
							rank,
							waitingAhead,
							totalWaitingCount
						);

					allUpdates.put(userId, response);
					rank++;

				} catch (Exception e) {
					log.error("개별 사용자 업데이트 준비 실패 - user: {}", tuple.getValue(), e);
				}
			}

			if (!allUpdates.isEmpty()) {
				WaitingQueueBatchEventResponse batchEvent = WaitingQueueBatchEventResponse.from(eventId, allUpdates);
				eventPublisher.publishEvent(batchEvent);
				log.info("실시간 순위 업데이트 완료 - eventId: {}, 대상: {}명", eventId, allUpdates.size());
			}
		} catch (Exception e) {
			log.error("실시간 순위 업데이트 실패 - userId: {}", eventId, e);
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

		//이미 결제된 경우
		if (status == QueueEntryStatus.COMPLETED) {
			throw new ErrorException(QueueEntryErrorCode.ALREADY_COMPLETED);
		}

		//대기중 상태가 아닌 경우
		if (status != QueueEntryStatus.WAITING) {
			throw new ErrorException(QueueEntryErrorCode.NOT_WAITING_STATUS);
		}
	}

	private void validatePaymentCompletion(QueueEntry queueEntry) {
		QueueEntryStatus status = queueEntry.getQueueEntryStatus();

		// 이미 결제 완료된 경우
		if (status == QueueEntryStatus.COMPLETED) {
			throw new ErrorException(QueueEntryErrorCode.ALREADY_COMPLETED);
		}

		// 만료된 경우
		if (status == QueueEntryStatus.EXPIRED) {
			throw new ErrorException(QueueEntryErrorCode.ALREADY_EXPIRED);
		}

		// 대기 중인 경우 (입장도 안 했는데 결제 시도)
		if (status == QueueEntryStatus.WAITING) {
			throw new ErrorException(QueueEntryErrorCode.NOT_ENTERED_STATUS);
		}

		// ENTERED 상태가 아닌 경우
		if (status != QueueEntryStatus.ENTERED) {
			throw new ErrorException(QueueEntryErrorCode.NOT_ENTERED_STATUS);
		}
	}


	private void updateRedis(Long eventId, Long userId) {
		try {
			queueEntryRedisRepository.moveToEnteredQueue(eventId, userId);
			queueEntryRedisRepository.incrementEnteredCount(eventId);
			log.debug("eventId {} - Redis 업데이트 성공", eventId);

		} catch (Exception e) {
			log.error("eventId {} - Redis 업데이트 실패", eventId);
		}
	}


	private void publishEnteredEvent(QueueEntry queueEntry) {
		EnteredQueueResponse response = EnteredQueueResponse.from(
			queueEntry.getUserId(),
			queueEntry.getEventId(),
			queueEntry.getEnteredAt(),
			queueEntry.getExpiredAt()
		);

		eventPublisher.publishEvent(response);
	}

	private void publishExpiredEvent(QueueEntry queueEntry) {
		ExpiredQueueResponse response = ExpiredQueueResponse.from(
			queueEntry.getUserId(),
			queueEntry.getEventId()
		);

		eventPublisher.publishEvent(response);
	}

	private void publishCompletedEvent(QueueEntry queueEntry) {
		CompletedQueueResponse response = CompletedQueueResponse.from(
			queueEntry.getUserId(),
			queueEntry.getEventId()
		);

		eventPublisher.publishEvent(response);

	}
}
