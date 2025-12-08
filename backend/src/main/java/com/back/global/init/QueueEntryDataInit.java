package com.back.global.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
@Order(3)
public class QueueEntryDataInit implements ApplicationRunner {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if(queueEntryRepository.count() > 0) {
			log.info("QueueEntry 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("QueueEntry 초기 데이터를 생성합니다.");

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. UserDataInit을 먼저 실행해주세요.");
			return;
		}


		Event event = eventRepository.findAll().stream()
			.findFirst()
			.orElse(null);

		if (event == null) {
			log.warn("Event 데이터가 없습니다. EventDataInit을 먼저 실행해주세요.");
			return;
		}

		// Event 상태를 QUEUE_READY로 변경
		event.changeStatus(EventStatus.QUEUE_READY);
		eventRepository.save(event);

		// QueueEntry 생성
		createTestQueueEntries(event, users);
		log.info("QueueEntry 생성 완료");

		// Redis 데이터 생성
		createTestRedisData(event.getId(), users);
	}

	/**
	 * 테스트 대기열 생성
	 * - WAITING: 50명 (1~50번)
	 * - ENTERED: 30명 (51~80번, 5분 전 입장)
	 * - EXPIRED: 20명 (81~100번, 20분 전 입장)
	 */
	private void createTestQueueEntries(Event event, List<User> users) {
		List<QueueEntry> entries = new ArrayList<>();

		// 1. WAITING 상태 50명 (1~50번)
		for (int i = 0; i < 50; i++) {
			QueueEntry entry = new QueueEntry(users.get(i), event, i + 1);
			entries.add(entry);
		}

		// 2. ENTERED 상태 30명 (51~80번)
		for (int i = 50; i < 80; i++) {
			QueueEntry entry = new QueueEntry(users.get(i), event, i + 1);
			entry.enterQueue(); // WAITING → ENTERED
			entries.add(entry);
		}

		// 3. EXPIRED 상태 20명 (81~100번)
		for (int i = 80; i < 100; i++) {
			QueueEntry entry = new QueueEntry(users.get(i), event, i + 1);
			entry.enterQueue();
			entry.expire(); // ENTERED → EXPIRED
			entries.add(entry);
		}

		queueEntryRepository.saveAll(entries);
		log.info("✅ QueueEntry DB 저장 완료: {}개", entries.size());
	}

	/**
	 * 테스트 Redis 데이터 생성 (Repository 메서드 사용)
	 */
	private void createTestRedisData(Long eventId, List<User> users) {
		try {
			// 1. WAITING 큐에 50명 추가 (1~50번)
			for (int i = 0; i < 50; i++) {
				queueEntryRedisRepository.addToWaitingQueue(
					eventId,
					users.get(i).getId(),
					i + 1
				);
			}
			log.info("✅ Redis WAITING 큐 저장: 50명");

			// 2. ENTERED 큐에 30명 추가 (51~80번) - 직접 추가 메서드 사용
			for (int i = 50; i < 80; i++) {
				queueEntryRedisRepository.addToEnteredQueueDirectly(
					eventId,
					users.get(i).getId()
				);
			}
			log.info("✅ Redis ENTERED 큐 저장: 30명 (TTL: 15분)");

			// 3. ENTERED 카운트 설정
			queueEntryRedisRepository.setEnteredCount(eventId, 30);
			log.info("✅ Redis ENTERED 카운트 저장: 30");

			log.info("✅ Redis 데이터 생성 완료 - eventId: {}", eventId);
		} catch (Exception e) {
			log.error("❌ Redis 데이터 생성 실패: {}", e.getMessage(), e);
		}
	}
}