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

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.CustomAuthenticationFilter;
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
public class MultiDeviceBlockAccessTokenTest {

	@Autowired
	CustomAuthenticationFilter filter;

	@Autowired
	UserHelper userHelper;

	@Autowired
	AuthTokenService authTokenService;

	@Autowired
	ActiveSessionRepository activeSessionRepository;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("기기 B가 로그인(rotate)하면, 기기 A의 기존 accessToken 요청은 ACCESS_OTHER_DEVICE로 차단된다")
	void block_old_access_token_after_other_device_login() throws Exception {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		ActiveSession session = activeSessionRepository
			.findByUserId(user.getId())
			.orElseGet(() -> activeSessionRepository.save(ActiveSession.create(user)));

		String sidA = session.getSessionId();
		long versionA = session.getTokenVersion();

		JwtDto deviceATokens = authTokenService.issueTokens(user, sidA, versionA);

		session.rotate();
		activeSessionRepository.save(session);
		authTokenService.issueTokens(user, session.getSessionId(), session.getTokenVersion());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.addHeader("Authorization", "");
		request.setCookies(
			new Cookie("accessToken", deviceATokens.accessToken()),
			new Cookie("refreshToken", deviceATokens.refreshToken())
		);

		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		filter.doFilter(request, response, new MockFilterChain());

		// then
		assertThat(response.getStatus()).isEqualTo(AuthErrorCode.ACCESS_OTHER_DEVICE.getHttpStatus().value());
		assertThat(response.getContentAsString()).contains(AuthErrorCode.ACCESS_OTHER_DEVICE.getMessage());
	}
}
