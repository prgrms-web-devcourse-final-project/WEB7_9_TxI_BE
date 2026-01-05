package com.back.global.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(4)
public class QueueEntryDataInit implements ApplicationRunner {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final PreRegisterRepository preRegisterRepository;
	private final EventRepository eventRepository;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if(queueEntryRepository.count() > 0) {
			log.info("QueueEntry 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("QueueEntry 초기 데이터를 생성합니다.");

		Long targetEventId = 1L; // 사전 등록된 이벤트

		Event event = eventRepository.findById(targetEventId)
			.orElse(null);

		if (event == null) {
			log.warn("ID {}에 해당하는 Event가 없습니다. Event를 먼저 생성해주세요.", targetEventId);
			return;
		}

		// REGISTERED 상태인 PreRegister 조회
		List<PreRegister> registeredPreRegisters = preRegisterRepository.findAll().stream()
			.filter(pr -> pr.getEventId().equals(targetEventId))
			.filter(PreRegister::isRegistered)
			.toList();

		if (registeredPreRegisters.isEmpty()) {
			log.warn("REGISTERED 상태의 PreRegister가 없습니다. PreRegisterDataInit을 먼저 실행해주세요.");
			return;
		}

		log.info("REGISTERED 상태의 PreRegister {}개를 발견했습니다.", registeredPreRegisters.size());

		// QueueEntry 생성
		createQueueEntriesFromPreRegisters(event, registeredPreRegisters);
		log.info("QueueEntry 생성 완료");

		// Redis 데이터 생성
		createRedisDataFromPreRegisters(event.getId(), registeredPreRegisters);
	}

	/**
	 * PreRegister 기반 대기열 생성
	 * - WAITING 상태로 생성
	 */
	private void createQueueEntriesFromPreRegisters(Event event, List<PreRegister> preRegisters) {
		List<QueueEntry> entries = new ArrayList<>();

		for (int i = 0; i < preRegisters.size(); i++) {
			PreRegister preRegister = preRegisters.get(i);
			QueueEntry entry = new QueueEntry(preRegister.getUser(), event, i + 1);
			entries.add(entry);
		}

		queueEntryRepository.saveAll(entries);
		log.info("QueueEntry DB 저장 완료: {}개", entries.size());
	}

	/**
	 * PreRegister 기반 Redis 데이터 생성
	 */
	private void createRedisDataFromPreRegisters(Long eventId, List<PreRegister> preRegisters) {
		try {
			for (int i = 0; i < preRegisters.size(); i++) {
				queueEntryRedisRepository.addToWaitingQueue(
					eventId,
					preRegisters.get(i).getUserId(),
					i + 1
				);
			}
			log.info("Redis WAITING 큐 저장: {}명", preRegisters.size());

			// ENTERED 카운트 초기화
			queueEntryRedisRepository.setEnteredCount(eventId, 0);
			log.info("Redis ENTERED 카운트 초기화: 0");

			log.info("Redis 데이터 생성 완료 - eventId: {}", eventId);
		} catch (Exception e) {
			log.error("Redis 데이터 생성 실패: {}", e.getMessage(), e);
		}
	}
}
