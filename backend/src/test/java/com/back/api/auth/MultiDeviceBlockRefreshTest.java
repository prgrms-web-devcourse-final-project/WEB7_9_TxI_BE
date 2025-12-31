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
	"custom.jwt.access-token-duration=1",
	"custom.jwt.refresh-token-duration=60"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class MultiDeviceBlockRefreshTest {

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
	@DisplayName("A의 access 만료 후 refresh 재발급 시도 중, B가 로그인(rotate)하면 ACCESS_OTHER_DEVICE 발생")
	void refresh_blocked_by_other_device_as_access_other_device() throws Exception {
		// given
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		// ActiveSession 준비
		ActiveSession session = activeSessionRepository.findByUserId(user.getId())
			.orElseGet(() -> activeSessionRepository.save(ActiveSession.create(user)));

		// 기기 A 토큰 발급
		JwtDto deviceATokens = authTokenService.issueTokens(user, session.getSessionId(), session.getTokenVersion());

		// access 만료 대기
		Thread.sleep(1100);

		// when: 기기 B 로그인 발생(rotate) + (싱글디바이스 정책이면) 기존 refresh 전부 revoke
		session.rotate();
		activeSessionRepository.save(session);
		refreshTokenRepository.revokeAllActiveByUserId(user.getId());

		// when: 기기 A가 만료된 access + 기존 refresh로 요청
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.addHeader("Authorization", ""); // (코드 수정했으면 없어도 됨)
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
