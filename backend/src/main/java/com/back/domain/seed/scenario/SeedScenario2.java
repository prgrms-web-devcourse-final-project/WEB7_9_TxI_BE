package com.back.domain.seed.scenario;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.api.queue.service.QueueShuffleService;
import com.back.api.seed.dto.ScenarioSeeder;
import com.back.api.seed.dto.response.SeedResult;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.seed.scenario.base.BaseSeedScenario;
import com.back.domain.seed.util.SeedResetSupport;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

@Component
@Profile({"perf", "dev"})
public class SeedScenario2 extends BaseSeedScenario implements ScenarioSeeder {

	public SeedScenario2(
		SeedResetSupport resetSupport,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		EventRepository eventRepository,
		PreRegisterRepository preRegisterRepository,
		QueueShuffleService queueShuffleService
	) {
		super(
			resetSupport,
			userRepository,
			passwordEncoder,
			eventRepository,
			preRegisterRepository,
			queueShuffleService
		);
	}

	@Override
	public String key() {
		return "preclosed_101_shuffled_open";
	}

	@Override
	public void reset() {
		resetAll();
	}

	@Override
	public SeedResult seed() {
		LocalDateTime now = LocalDateTime.now();

		// 티켓 오픈 "시점" (이미 지난 시간으로 두면 EventOpenScheduler가 즉시 OPEN으로 바꿀 대상)
		LocalDateTime ticketOpenAt = now.minusSeconds(30);

		Event event = createEvent(
			now,
			"PRE-CLOSED + 101 PR + SHUFFLED + OPEN DUE",
			ticketOpenAt,
			EventStatus.PRE_OPEN
		);

		List<User> users = createUsersN(100);
		createPreRegisters(event, users);
		shuffleQueue(event, users); // Redis waiting + status QUEUE_READY

		// 여기서는 OPEN으로 직접 바꾸지 않음.
		// -> EventOpenScheduler가 돌면 QUEUE_READY + ticketOpenAt <= now 조건으로 OPEN 전환됨.

		return new SeedResult(
			key(),
			event.getId(),
			users.size(),
			users.size(),
			true,
			"ticketOpenAt=" + ticketOpenAt + ", eventStatus=" + event.getStatus() + " (OPEN scheduler target)"
		);
	}
}
