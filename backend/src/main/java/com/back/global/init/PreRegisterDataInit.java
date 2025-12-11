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
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(3)
public class PreRegisterDataInit implements ApplicationRunner {

	private final PreRegisterRepository preRegisterRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (preRegisterRepository.count() > 0) {
			log.info("PreRegister 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("PreRegister 초기 데이터를 생성합니다.");

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. UserDataInit을 먼저 실행해주세요.");
			return;
		}

		Long targetEventId = 4L; //사전 등록 중인 이벤트

		Event event = eventRepository.findById(targetEventId)
			.orElse(null);

		if (event == null) {
			log.warn("ID {}에 해당하는 Event가 없습니다. Event를 먼저 생성해주세요.", targetEventId);
			return;
		}

		List<PreRegister> preRegisters = createTestPreRegisters(event, users, 150);

	}

	public List<PreRegister> createTestPreRegisters(Event event, List<User> users, int count) {
		List<PreRegister> preRegisters = new ArrayList<>();

		int registerCount = Math.min(count, users.size());

		for (int i = 0; i < registerCount; i++) {
			PreRegister preRegister = PreRegister.builder()
				.event(event)
				.user(users.get(i))
				.build();
			preRegisters.add(preRegister);
		}

		preRegisterRepository.saveAll(preRegisters);

		return preRegisters;

	}
}
