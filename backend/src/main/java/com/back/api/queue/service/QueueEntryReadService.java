package com.back.api.queue.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.service.EventService;
import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.EnteredQueueResponse;
import com.back.api.queue.dto.response.ExpiredQueueResponse;
import com.back.api.queue.dto.response.QueueEntryListResponse;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.dto.response.QueueStatisticsResponse;
import com.back.api.queue.dto.response.WaitingQueueResponse;
import com.back.domain.event.entity.Event;
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
 * TODO 시간 계산 로직 수정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QueueEntryReadService {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final EventService eventService;

	public QueueEntryStatusResponse getMyQueueStatus(Long eventId, Long userId) {
		QueueEntry entry = queueEntryRepository.findByEvent_IdAndUser_Id(eventId, userId)
			.orElseThrow(() -> new ErrorException(QueueEntryErrorCode.NOT_FOUND_QUEUE_ENTRY));

		return switch (entry.getQueueEntryStatus()) {
			case WAITING -> buildWaitingQueueResponse(eventId, entry);
			case ENTERED -> buildEnteredQueueResponse(entry);
			case EXPIRED -> buildExpiredQueueResponse(entry);
			case COMPLETED -> buildCompletedQueueResponse(entry);
		};
	}

	public List<QueueEntry> getMyAllQueues(Long userId) {
		return queueEntryRepository.findAllByUserId(userId);
	}

	public List<QueueEntry> getWaitingOrEnteredQueues(Long userId) {
		return getMyAllQueues(userId)
			.stream()
			.filter(queue ->
				queue.getQueueEntryStatus().equals(QueueEntryStatus.WAITING)
					|| queue.getQueueEntryStatus().equals(QueueEntryStatus.ENTERED))
			.toList();
	}

	//Redis 조회 + 계산
	//단일 사용자 조회 (API에서 사용 예정)
	public WaitingQueueResponse buildWaitingQueueResponseForUser(Long eventId, Long userId) {
		Long currentRank = queueEntryRedisRepository.getMyRankInWaitingQueue(eventId, userId);
		Long waitingAheadCount = queueEntryRedisRepository.getWaitingAheadCount(eventId, userId);
		Long totalWaitingCount = queueEntryRedisRepository.getTotalWaitingCount(eventId);

		//Redis에 데이터가 없으면 null
		if (currentRank == null || waitingAheadCount == null || totalWaitingCount == null) {
			return null;
		}

		return buildWaitingQueueResponseFromRank(
			userId,
			eventId,
			currentRank.intValue(),
			waitingAheadCount.intValue(),
			totalWaitingCount.intValue()
		);
	}

	public WaitingQueueResponse buildWaitingQueueResponseFromRank(
		Long userId,
		Long eventId,
		int rank,
		int waitingAhead,
		int totalWaitingCount
	) {
		int estimatedWaitTime;
		int progress;

		// 1순위 사용자 처리
		if (waitingAhead == 0) {
			estimatedWaitTime = 1;
			progress = 99;
		} else {
			estimatedWaitTime = waitingAhead * 2;
			progress = totalWaitingCount > 0
				? (int)(((totalWaitingCount - waitingAhead) * 100) / totalWaitingCount)
				: 0;
		}

		return WaitingQueueResponse.from(
			userId,
			eventId,
			rank,
			waitingAhead,
			estimatedWaitTime,
			progress
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
			boolean isInRedis = queueEntryRedisRepository.isInEnteredQueue(eventId, userId);

			if (isInRedis) {
				return true;
			}

			//Redis false면 DB 한번 더 확인
			return queueEntryRepository
				.findByEvent_IdAndUser_Id(eventId, userId)
				.map(entry -> entry.getQueueEntryStatus() == QueueEntryStatus.ENTERED)
				.orElse(false);

		} catch (Exception e) {
			// Redis 예외 시 DB 조회
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

		long completedCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId,
			QueueEntryStatus.COMPLETED
		);

		return QueueStatisticsResponse.from(
			eventId,
			totalWaitingCount,
			waitingCount,
			enteredCount,
			expiredCount,
			completedCount
		);
	}

	// 관리자용 - 대기열 전체 조회
	public Page<QueueEntryListResponse> getQueueEntriesByEventId(Long eventId, int page, int size) {

		Event event = eventService.getEventEntity(eventId);

		Pageable pageable = PageRequest.of(page, size);
		Page<QueueEntry> entries = queueEntryRepository.findByEventIdWithUserAndEvent(eventId, pageable);
		return entries.map(QueueEntryListResponse::from);
	}

	//Redis 먼저 조회
	private WaitingQueueResponse buildWaitingQueueResponse(Long eventId, QueueEntry entry) {

		WaitingQueueResponse response = buildWaitingQueueResponseForUser(eventId, entry.getUserId());

		//Redis에 데이터가 없으면 DB 기반 응답 생성
		if (response == null) {
			return buildWaitingQueueResponseFromDB(eventId, entry);
		}

		return response;
	}

	//DB 기반
	private WaitingQueueResponse buildWaitingQueueResponseFromDB(Long eventId, QueueEntry entry) {
		long waitingAheadCount = queueEntryRepository
			.countByEvent_IdAndQueueRankLessThan(eventId, entry.getQueueRank());
		long totalWaitingCount = queueEntryRepository.countByEvent_IdAndQueueEntryStatus(
			eventId, QueueEntryStatus.WAITING
		);

		return buildWaitingQueueResponseFromRank(
			entry.getUserId(),
			entry.getEventId(),
			entry.getQueueRank(),
			(int)waitingAheadCount,
			(int)totalWaitingCount
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
}
