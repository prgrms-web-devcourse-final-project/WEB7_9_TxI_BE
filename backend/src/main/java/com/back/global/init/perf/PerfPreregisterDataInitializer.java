package com.back.global.init.perf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfPreregisterDataInitializer {

	private final PreRegisterRepository preRegisterRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	public void init(double preregRatio) {
		if (preRegisterRepository.count() > 0) {
			log.info("PreRegister 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. PerfUserDataInitializer를 먼저 실행해주세요.");
			return;
		}

		List<Event> events = eventRepository.findAll();
		if (events.isEmpty()) {
			log.warn("Event 데이터가 없습니다. PerfEventDataInitializer를 먼저 실행해주세요.");
			return;
		}

		// Event #1 (PRE_OPEN) 조회
		Event event1 = eventRepository.findById(1L).orElse(null);
		if (event1 == null || event1.getStatus() != EventStatus.PRE_OPEN) {
			log.warn("Event #1이 없거나 PRE_OPEN 상태가 아닙니다. 사전등록 생성을 건너뜁니다.");
			return;
		}

		log.info("PreRegister 초기 데이터 생성 중: Event #1 ({}) 전용, 비율 {}%",
			event1.getTitle(), (int)(preregRatio * 100));

		// Event #1에만 사전등록 생성
		int preRegisterCount = (int)(users.size() * preregRatio);
		List<PreRegister> preRegisters = createPreRegistersForEvent(event1, users, preRegisterCount);
		preRegisterRepository.saveAll(preRegisters);

		log.info("✅ PreRegister 데이터 생성 완료: Event #1에 {}건 (사전등록 부하테스트용)", preRegisters.size());
	}

	private List<PreRegister> createPreRegistersForEvent(Event event, List<User> users, int count) {
		List<PreRegister> preRegisters = new ArrayList<>();

		int registerCount = Math.min(count, users.size());

		for (int i = 0; i < registerCount; i++) {
			User user = users.get(i);

			PreRegister preRegister = PreRegister.builder()
				.event(event)
				.user(user)
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();

			preRegisters.add(preRegister);
		}

		return preRegisters;
	}
}
