package com.back.api.queue.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.EnteredQueueResponse;
import com.back.api.queue.dto.response.ExpiredQueueResponse;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.dto.response.QueueStatisticsResponse;
import com.back.api.queue.dto.response.WaitingQueueResponse;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 조회 로직
 * Redis 우선 조회 -> DB 조회
 * 트랜잭션 읽기 / 쓰기 분리 고려
 * TODO 사용자 / 관리자 권한 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QueueEntryReadService {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	public QueueEntryStatusResponse getMyQueueStatus(Long eventId, Long userId) {
		QueueEntry entry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(()-> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		return switch(entry.getQueueEntryStatus()) {
			case WAITING -> buildWaitingQueueResponse(eventId, entry);
			case ENTERED  -> buildEnteredQueueResponse(entry);
			case EXPIRED  -> buildExpiredQueueResponse(entry);
			case COMPLETED -> buildCompletedQueueResponse(entry);
		};
	}

	//Redis 먼저 조회
	private WaitingQueueResponse buildWaitingQueueResponse(Long eventId, QueueEntry entry) {
		Long currentRank = queueEntryRedisRepository.getMyRankInWaitingQueue(eventId, entry.getUserId());
		Long waitingAheadCount = queueEntryRedisRepository.getWaitingAheadCount(eventId, entry.getUserId());
		Long totalWaitingCount = queueEntryRedisRepository.getTotalWaitingCount(eventId);

		//Redis에 데이터가 없으면 DB 기반 응답 생성
		if (currentRank == null) {
			return buildWaitingQueueResponseFromDB(eventId, entry);
		}

		//TODO 시간 계산 로직 수정
		int estimatedWaitTime = (int) (waitingAheadCount * 3); //대기 인원당 3분 가정

		int progress = totalWaitingCount > 0
			? (int) (((totalWaitingCount - waitingAheadCount) * 100) / totalWaitingCount)
			: 0;


		return WaitingQueueResponse.from(
			entry.getUserId(),
			entry.getEventId(),
			currentRank.intValue(),
			waitingAheadCount.intValue(),
			estimatedWaitTime,
			progress
		);
	}

	//DB 기반
	private WaitingQueueResponse buildWaitingQueueResponseFromDB(Long eventId, QueueEntry entry) {
		long waitingAheadCount = queueEntryRepository
			.countByEvent_IdAndQueueRankLessThan(eventId, entry.getQueueRank());
		long totalWaitingCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId, QueueEntryStatus.WAITING
		);

		int estimatedWaitTime = (int) (waitingAheadCount * 3); //대기 인원당 3분 가정

		int progress = totalWaitingCount > 0
			? (int) (((totalWaitingCount - waitingAheadCount) * 100) / totalWaitingCount)
			: 0;

		return WaitingQueueResponse.from(
			entry.getUserId(),
			entry.getEventId(),
			entry.getQueueRank(),
			(int)waitingAheadCount,
			estimatedWaitTime,
			progress
		);
	}

	private EnteredQueueResponse buildEnteredQueueResponse(QueueEntry entry) {
		return EnteredQueueResponse.from(
			entry.getUserId(),
			entry.getEventId(),
			entry.getEnteredAt(),
			entry.getExpiredAt()
		);
	}

	private ExpiredQueueResponse buildExpiredQueueResponse(QueueEntry entry) {
		return ExpiredQueueResponse.from(
			entry.getUserId(),
			entry.getEventId()
		);
	}

	private CompletedQueueResponse buildCompletedQueueResponse(QueueEntry entry) {
		return CompletedQueueResponse.from(
			entry.getUserId(),
			entry.getEventId()
		);
	}

	//대기열에 있는 지 확인
	public boolean existsInWaitingQueue(Long eventId, Long userId) {
		try {
			if (queueEntryRedisRepository.isInWaitingQueue(eventId, userId)
				|| queueEntryRedisRepository.isInEnteredQueue(eventId, userId)) {
				return true;
			}
		} catch (Exception e) {
			log.warn("Redis 조회 실패, DB Fallback");
		}
		return queueEntryRepository.existsByEvent_IdAndUser_Id(eventId, userId);
	}

	//대기열 ENTERED 상태인지 확인
	//Redis & DB
	public boolean isUserEntered(Long eventId, Long userId) {
		try {
			return queueEntryRedisRepository.isInEnteredQueue(eventId, userId);
		} catch (Exception e) {
			log.warn("Redis ENTERED 조회 실패, DB Fallback");

			return queueEntryRepository
				.findByEvent_IdAndUser_Id(eventId, userId)
				.map(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.ENTERED)
				.orElse(false);
		}
	}


	public QueueStatisticsResponse getQueueStatistics(Long eventId) {
		long totalWaitingCount = queueEntryRepository.countByEvent_Id(eventId);

		if (totalWaitingCount == 0) {
			throw new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY);
		}


		long waitingCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId,
			QueueEntryStatus.WAITING
		);

		long enteredCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId,
			QueueEntryStatus.ENTERED
		);

		long expiredCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId,
			QueueEntryStatus.EXPIRED
		);

		return QueueStatisticsResponse.from(
			eventId,
			totalWaitingCount,
			waitingCount,
			enteredCount,
			expiredCount
		);

	}

}
