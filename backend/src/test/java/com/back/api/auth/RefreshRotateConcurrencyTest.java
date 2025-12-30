package com.back.api.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.config.TestRedisConfig;
import com.back.config.TestRequestMetaConfig;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfig.class, TestRequestMetaConfig.class})
class RefreshRotateConcurrencyTest {

	@Autowired
	UserHelper userHelper;
	@Autowired
	AuthTokenService authTokenService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	ActiveSessionRepository activeSessionRepository;

	@Test
	@DisplayName("동일한 refresh token으로 동시에 2번 재발급 요청이 들어오면, 1번만 성공하고 나머지는 차단")
	void rotate_concurrently_only_one_succeeds() throws Exception {
		// given
		TestUser testUser = userHelper.createUser(UserRole.NORMAL);
		User user = testUser.user();

		ActiveSession session = activeSessionRepository.save(ActiveSession.create(user));
		JwtDto issued = authTokenService.issueTokens(user, session.getSessionId(), session.getTokenVersion());

		String refresh = issued.refreshToken();

		int threads = 2;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);

		List<Future<Object>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			futures.add(pool.submit(() -> {
				ready.countDown();
				start.await();
				try {
					return authTokenService.rotateTokenByRefreshToken(refresh);
				} catch (ErrorException e) {
					return e;
				}
			}));
		}

		ready.await();
		start.countDown();

		int success = 0;
		List<AuthErrorCode> errorCodes = new ArrayList<>();

		for (Future<Object> f : futures) {
			Object r = f.get(5, TimeUnit.SECONDS);

			if (r instanceof JwtDto) {
				success++;
				continue;
			}

			if (r instanceof ErrorException ee) {
				errorCodes.add((AuthErrorCode)ee.getErrorCode());
				continue;
			}

			fail("Unexpected result type: " + (r == null ? "null" : r.getClass().getName()));
		}

		pool.shutdownNow();

		assertThat(success)
			.as("Exactly one rotate should succeed (success=%s, errors=%s)", success, errorCodes)
			.isEqualTo(1);

		assertThat(errorCodes)
			.as("Exactly one rotate should fail (success=%s, errors=%s)", success, errorCodes)
			.hasSize(1);

		assertThat(errorCodes.get(0)).isEqualTo(AuthErrorCode.ACCESS_OTHER_DEVICE);
	}
}
