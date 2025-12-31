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

import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.JwtProvider;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=1",
	"custom.jwt.refresh-token-duration=60"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SessionMissingOnRefreshRotateTest {

	@Autowired
	CustomAuthenticationFilter filter;
	@Autowired
	UserHelper userHelper;
	@Autowired
	AuthTokenService authTokenService;
	@Autowired
	ActiveSessionRepository activeSessionRepository;

	@Autowired
	JwtProvider jwtProvider;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("ActiveSession이 없으면(refresh rotate) UNAUTHORIZED")
	void rotate_refresh_fails_when_active_session_missing() throws Exception {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		activeSessionRepository.findByUserId(user.getId()).ifPresent(activeSessionRepository::delete);

		String sid = "test-sid";
		long version = 1L;

		String access = jwtProvider.generateAccessToken(user, sid, version);
		String refresh = jwtProvider.generateRefreshToken(user, sid, version);

		Thread.sleep(1100); // access 만료시키기

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(new Cookie("accessToken", access), new Cookie("refreshToken", refresh));

		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		filter.doFilter(request, response, new MockFilterChain());

		// then
		assertThat(response.getStatus()).isEqualTo(AuthErrorCode.UNAUTHORIZED.getHttpStatus().value());
		assertThat(response.getContentAsString()).contains(AuthErrorCode.UNAUTHORIZED.getMessage());
	}
}
