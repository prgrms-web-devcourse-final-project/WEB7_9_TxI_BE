package com.back.api.auth;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=60",
	"custom.jwt.refresh-token-duration=60"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class RefreshOneTimeUseTest {

	@Autowired
	UserHelper userHelper;
	@Autowired
	AuthTokenService authTokenService;
	@Autowired
	ActiveSessionRepository activeSessionRepository;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("같은 refreshToken으로 rotate를 두 번 시도하면 두 번째는 REFRESH_TOKEN_NOT_FOUND")
	void rotate_twice_should_fail_second_time() {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		ActiveSession session = activeSessionRepository.findByUserId(user.getId())
			.orElseGet(() -> activeSessionRepository.save(ActiveSession.create(user)));

		JwtDto tokens = authTokenService.issueTokens(user, session.getSessionId(), session.getTokenVersion());

		// 1회차 rotate 성공
		JwtDto rotated = authTokenService.rotateTokenByRefreshToken(tokens.refreshToken());
		assertThat(rotated.accessToken()).isNotNull();

		// 2회차 rotate 실패 (이미 revoke 처리됨)
		assertThatThrownBy(() -> authTokenService.rotateTokenByRefreshToken(tokens.refreshToken()))
			.isInstanceOf(ErrorException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.ACCESS_OTHER_DEVICE);
	}
}
