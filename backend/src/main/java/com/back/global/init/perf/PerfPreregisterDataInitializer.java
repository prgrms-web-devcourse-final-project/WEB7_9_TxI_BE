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

		int eventCount = events.size();
		int totalPreRegisterCount = (int) (users.size() * preregRatio);

		log.info("PreRegister 초기 데이터 생성 중: 총 {}개 이벤트에 {}명 균등 분배, 비율 {}%",
			eventCount, totalPreRegisterCount, (int) (preregRatio * 100));

		// 모든 이벤트에 골고루 분배 (Round-robin 방식)
		List<PreRegister> preRegisters = new ArrayList<>();
		int userIndex = 0;

		for (int i = 0; i < totalPreRegisterCount && userIndex < users.size(); i++) {
			Event event = events.get(i % eventCount); // Round-robin으로 이벤트 선택
			User user = users.get(userIndex);

			PreRegister preRegister = PreRegister.builder()
				.event(event)
				.user(user)
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();

			preRegisters.add(preRegister);
			userIndex++;
		}

		preRegisterRepository.saveAll(preRegisters);

		// 이벤트별 생성 개수 로깅
		for (Event event : events) {
			long count = preRegisters.stream()
				.filter(pr -> pr.getEvent().getId().equals(event.getId()))
				.count();
			log.info("  - Event #{} ({}): {}건", event.getId(), event.getTitle(), count);
		}

		log.info("✅ PreRegister 데이터 생성 완료: 총 {}건 ({}개 이벤트에 균등 분배)",
			preRegisters.size(), eventCount);
	}
}
