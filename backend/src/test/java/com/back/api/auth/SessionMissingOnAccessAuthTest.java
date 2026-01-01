package com.back.api.auth;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.auth.repository.RefreshTokenRedisRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.JwtProvider;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=60",
	"custom.jwt.refresh-token-duration=60"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SessionMissingOnAccessAuthTest {

	@Autowired
	CustomAuthenticationFilter filter;
	@Autowired
	UserHelper userHelper;
	@Autowired
	JwtProvider jwtProvider;
	@Autowired
	ActiveSessionRepository activeSessionRepository;
	@Autowired
	RefreshTokenRedisRepository refreshTokenRedisRepository;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("ActiveSession이 없으면(access 인증 단계) UNAUTHORIZED")
	void access_auth_fails_when_active_session_missing() throws Exception {
		// given
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		// 세션 제거 (없도록 보장)
		activeSessionRepository.findByUserId(user.getId())
			.ifPresent(activeSessionRepository::delete);

		String sid = "test-sid";
		long version = 1L;

		// 만료되지 않은 accessToken을 만들고
		String access = jwtProvider.generateAccessToken(user, sid, version);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// request에 accessToken을 반드시 실어 보낸다 (쿠키 or Authorization 중 택1)
		request.setCookies(new Cookie("accessToken", access));
		// 또는:
		// request.addHeader("Authorization", "Bearer " + access);

		// when
		filter.doFilter(request, response, new MockFilterChain());

		// then
		assertThat(response.getStatus())
			.isEqualTo(AuthErrorCode.UNAUTHORIZED.getHttpStatus().value());

		assertThat(response.getContentAsString())
			.contains(AuthErrorCode.UNAUTHORIZED.getMessage());
	}
}
