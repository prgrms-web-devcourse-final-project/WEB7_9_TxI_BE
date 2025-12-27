package com.back.global.init.perf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfUserDataInitializer {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ActiveSessionRepository activeSessionRepository;

	public void init(int userCount) {
		if (userRepository.count() > 0) {
			log.info("User 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("User 초기 데이터 생성 중: {}명", userCount);

		List<User> users = createTestUsers(userCount);

		User admin = User.builder()
			.email("admin@test.com")
			.password(passwordEncoder.encode("admin1234"))
			.nickname("admin")
			.fullName("admin")
			.role(UserRole.ADMIN)
			.birthDate(LocalDate.of(1990, 1, 1))
			.activeStatus(UserActiveStatus.ACTIVE)
			.build();

		userRepository.save(admin);

		// ActiveSession 생성 (k6 테스트용)
		createActiveSessions(users);

		// admin용 고정 sessionId
		String adminSessionId = String.format("00000000-0000-0000-0000-%012d", admin.getId());
		ActiveSession adminSession = ActiveSession.builder()
			.user(admin)
			.sessionId(adminSessionId)
			.tokenVersion(1L)
			.lastLoginAt(java.time.LocalDateTime.now())
			.build();
		activeSessionRepository.save(adminSession);

		log.info("✅ User 데이터 생성 완료: 일반 사용자 {}명 + 관리자 1명", users.size());
		log.info("✅ ActiveSession 생성 완료: {}개", users.size() + 1);
	}

	private List<User> createTestUsers(int count) {
		List<User> users = new ArrayList<>();

		for (int i = 1; i <= count; i++) {
			User user = User.builder()
				.email("test" + i + "@test.com")
				.password(passwordEncoder.encode("abc12345"))
				.nickname("test" + i)
				.fullName("test user " + i)
				.role(UserRole.NORMAL)
				.birthDate(LocalDate.of(2000, 1, 1))
				.activeStatus(UserActiveStatus.ACTIVE)
				.build();
			users.add(user);
		}

		return userRepository.saveAll(users);
	}

	private void createActiveSessions(List<User> users) {
		List<ActiveSession> sessions = users.stream()
			.map(user -> {
				// k6 테스트용 고정 sessionId (userId 기반)
				String fixedSessionId = String.format("00000000-0000-0000-0000-%012d", user.getId());
				return ActiveSession.builder()
					.user(user)
					.sessionId(fixedSessionId)
					.tokenVersion(1L)
					.lastLoginAt(java.time.LocalDateTime.now())
					.build();
			})
			.toList();
		activeSessionRepository.saveAll(sessions);
	}
}
