package com.back.api.queue.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.service.EventService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.QueueEntryErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 대기열 랜덤 큐 생성 로직
 * 사전 등록 완료 후 대기열 섞기 기능을 통해 랜덤 큐를 생성
 * 자동으로 섞기 + 관리자 전용 수동 섞기
 * 공정한 대기열 생성 로직 논의 필요 -> 현재는 SecureRandom 이용한 랜덤 섞기 로직으로 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueShuffleService {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final UserRepository userRepository;
	private final EventService eventService;

	@Transactional
	public void shuffleQueue(Long eventId, List<Long> preRegisteredUserIds) {

		Event event = eventService.getEventEntity(eventId);

		validateShuffleRequest(eventId, preRegisteredUserIds);

		List<Long> shuffledUserIds = shuffleUserIds(preRegisteredUserIds);
		List<User> users = userRepository.findAllById(preRegisteredUserIds);

		if (users.size() != shuffledUserIds.size()) {
			throw new ErrorException(QueueEntryErrorCode.INVALID_PREREGISTER_LIST);
		}

		saveToRedis(eventId, shuffledUserIds);
		saveToDatabase(event, users, shuffledUserIds);

		event.changeStatus(EventStatus.QUEUE_READY);

		//TODO 알림 로직 추가 필요

	}

	private void validateShuffleRequest(Long eventId, List<Long> preRegisteredUserIds) {

		if (preRegisteredUserIds == null || preRegisteredUserIds.isEmpty()) {
			throw new ErrorException(QueueEntryErrorCode.PRE_REGISTERED_USERS_EMPTY);
		}

		// 대기열 중복 확인
		long registeredCount = queueEntryRepository.countByEvent_Id(eventId);
		if (registeredCount > 0) {
			throw new ErrorException(QueueEntryErrorCode.QUEUE_ALREADY_EXISTS);
		}
	}

	//Fisher-Yates Shuffle (SecureRandom 기반) -> 추후 더욱 공정한 로직으로 변경한다.
	private List<Long> shuffleUserIds(List<Long> userIds) {
		List<Long> shuffledList = new ArrayList<>(userIds);
		SecureRandom secureRandom = new SecureRandom();

		for (int i = shuffledList.size() - 1; i > 0; i--) {
			int inx = secureRandom.nextInt(i + 1); //0~i 사이 랜덤 인덱스
			Long tmp = shuffledList.get(i);
			shuffledList.set(i, shuffledList.get(inx));
			shuffledList.set(inx, tmp);
		}

		return shuffledList;
	}

	private void saveToRedis(Long eventId, List<Long> shuffledUserIds) {
		try {
			for (int i = 0; i < shuffledUserIds.size(); i++) {
				Long userId = shuffledUserIds.get(i);
				int rank = i + 1;
				queueEntryRedisRepository.addToWaitingQueue(eventId, userId, rank);
			}
			log.debug("eventId {} - Redis 저장 성공", eventId);
		} catch (Exception e) {
			log.error("eventId {} - Redis 저장 실패", eventId);
			throw new ErrorException(QueueEntryErrorCode.REDIS_CONNECTION_FAILED);
		}
	}

	private void saveToDatabase(Event event, List<User> users, List<Long> shuffledUserIds) {

		//UserId로 User 매핑
		Map<Long, User> userMap = new HashMap<>();
		users.forEach(user -> userMap.put(user.getId(), user));

		List<QueueEntry> entries = new ArrayList<>();

		for (int i = 0; i < shuffledUserIds.size(); i++) {
			int rank = i + 1;
			Long userId = shuffledUserIds.get(i);
			User user = userMap.get(userId);

			QueueEntry queueEntry = QueueEntry.builder()
				.event(event)
				.user(user)
				.queueRank(rank)
				.queueEntryStatus(QueueEntryStatus.WAITING)
				.build();
			entries.add(queueEntry);
		}
		queueEntryRepository.saveAll(entries);
	}
}
