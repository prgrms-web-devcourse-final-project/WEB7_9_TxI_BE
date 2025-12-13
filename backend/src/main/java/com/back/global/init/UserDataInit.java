package com.back.global.init;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(1)
public class UserDataInit implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;


	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			log.info("User 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("User 초기 데이터를 생성합니다.");

		List<User> users = createTestUsers(150);

		User admin = User.builder()
			.email("admin@test.com")
			.password(passwordEncoder.encode("admin1234"))
			.nickname("admin")
			.role(UserRole.ADMIN)
			.birthDate(LocalDate.of(1990, 1, 1))
			.activeStatus(UserActiveStatus.ACTIVE)
			.build();

		userRepository.save(admin);

		log.info("User 초기 데이터 {}명이 생성되었습니다.", users.size());
	}

	private List<User> createTestUsers(int count) {
		List<User> users = new ArrayList<>();

		for (int i = 1; i <= count; i++) {
			User user = User.builder()
				.email("test" + i + "@test.com")
				.password(passwordEncoder.encode("abc12345"))
				.nickname("test" + i)
				.role(UserRole.NORMAL)
				.birthDate(LocalDate.of(2000, 1, 1))
				.activeStatus(UserActiveStatus.ACTIVE)
				.build();
			users.add(user);
		}

		return userRepository.saveAll(users);
	}
}
