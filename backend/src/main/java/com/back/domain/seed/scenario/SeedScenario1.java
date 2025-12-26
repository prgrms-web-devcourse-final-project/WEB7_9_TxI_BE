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
public class SeedScenario1 extends BaseSeedScenario implements ScenarioSeeder {

	public SeedScenario1(
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
		return "preclosed_101_shuffled_before_open";
	}

	@Override
	public void reset() {
		resetAll();
	}

	@Override
	public SeedResult seed() {
		LocalDateTime now = LocalDateTime.now();

		LocalDateTime ticketOpenAt = now.plusMinutes(30);

		Event event = createEvent(
			now,
			"PRE-CLOSED + 101 PR + SHUFFLED + BEFORE OPEN",
			ticketOpenAt,
			EventStatus.PRE_OPEN
		);

		List<User> users = createUsersN(101);
		createPreRegisters(event, users);
		shuffleQueue(event, users);

		return new SeedResult(
			key(),
			event.getId(),
			users.size(),
			users.size(),
			true,
			"ticketOpenAt=" + ticketOpenAt + ", eventStatus=" + event.getStatus()
		);
	}
}
