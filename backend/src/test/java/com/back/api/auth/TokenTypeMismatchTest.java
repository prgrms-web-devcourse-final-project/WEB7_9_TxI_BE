package com.back.api.auth;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.config.SecurityConfig;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.security.JwtProvider;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SecurityConfig.class) // UserHelper에서 PasswordEncoder 주입을 위해 필요
class TokenTypeMismatchTest {

	@Autowired
	AuthTokenService authTokenService;
	@Autowired
	JwtProvider jwtProvider;
	@Autowired
	UserHelper userHelper;
	@Autowired
	ActiveSessionRepository activeSessionRepository;

	@Test
	@DisplayName("rotateTokenByRefreshToken에 accessToken을 넣으면 INVALID_TOKEN")
	void rotate_with_access_token_should_be_invalid() {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		ActiveSession session = activeSessionRepository.findByUserId(user.getId())
			.orElseGet(() -> activeSessionRepository.save(ActiveSession.create(user)));

		String access = jwtProvider.generateAccessToken(user, session.getSessionId(), session.getTokenVersion());

		assertThatThrownBy(() -> authTokenService.rotateTokenByRefreshToken(access))
			.isInstanceOf(ErrorException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_TOKEN);
	}
}
