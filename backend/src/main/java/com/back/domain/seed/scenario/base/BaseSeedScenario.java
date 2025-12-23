package com.back.domain.seed.scenario.base;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.api.queue.service.QueueShuffleService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.seed.util.SeedResetSupport;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseSeedScenario {

	protected static final String PREFIX = "[SEED] ";

	protected final SeedResetSupport resetSupport;
	protected final UserRepository userRepository;
	protected final PasswordEncoder passwordEncoder;
	protected final EventRepository eventRepository;
	protected final PreRegisterRepository preRegisterRepository;
	protected final QueueShuffleService queueShuffleService;

	protected void resetAll() {
		resetSupport.resetSeedDataByPrefix(PREFIX);
	}

	protected List<User> createUsersN(int userCount) {
		List<User> users = new ArrayList<>(userCount);
		String encoded = passwordEncoder.encode("abc12345");

		for (int i = 1; i <= userCount; i++) {
			User user = User.builder()
				.email("seed" + i + "@test.com")
				.password(encoded)
				.fullName("Seed User " + i)
				.nickname("seedNick" + i)
				.role(UserRole.NORMAL)
				.activeStatus(UserActiveStatus.ACTIVE)
				.birthDate(LocalDate.of(2000, 1, 1))
				.build();

			users.add(user);
		}

		return userRepository.saveAll(users);
	}

	protected Event createEvent(LocalDateTime now, String titleSuffix,
		LocalDateTime ticketOpenAt, EventStatus status) {

		// 사전등록은 "마감" 상태로 만들 거라서 preCloseAt은 now보다 과거
		LocalDateTime preOpenAt = now.minusDays(7);
		LocalDateTime preCloseAt = now.minusMinutes(1);

		LocalDateTime ticketCloseAt = ticketOpenAt.plusHours(2);
		LocalDateTime eventDate = now.plusDays(10);

		Event event = Event.builder()
			.title(PREFIX + titleSuffix)
			.category(EventCategory.CONCERT)
			.description("seed data")
			.place("seed place")
			.imageUrl(null)
			.minPrice(10000)
			.maxPrice(50000)
			.preOpenAt(preOpenAt)
			.preCloseAt(preCloseAt)
			.ticketOpenAt(ticketOpenAt)
			.ticketCloseAt(ticketCloseAt)
			.eventDate(eventDate)
			.maxTicketAmount(1000)
			.status(status)
			.build();

		return eventRepository.save(event);
	}

	protected void createPreRegisters(Event event, List<User> users) {
		List<PreRegister> preRegisters = users.stream()
			.map(user -> PreRegister.builder()
				.event(event)
				.user(user)
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build()
			)
			.toList();

		preRegisterRepository.saveAll(preRegisters);
	}

	protected void shuffleQueue(Event event, List<User> users) {
		List<Long> userIds = users.stream().map(User::getId).toList();
		queueShuffleService.shuffleQueue(event.getId(), userIds);

		event.changeStatus(EventStatus.QUEUE_READY);
		eventRepository.save(event);
	}
}
