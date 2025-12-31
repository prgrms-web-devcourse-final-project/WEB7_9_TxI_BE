package com.back.api.auth;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.SecurityUser;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=1",
	"custom.jwt.refresh-token-duration=60"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class CustomAuthenticationFilterReissueTest {

	@Autowired
	private CustomAuthenticationFilter filter;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private ActiveSessionRepository activeSessionRepository;

	@Value("${custom.jwt.secret}")
	private String secret;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	static class CapturingChain implements FilterChain {
		Authentication captured;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {
			captured = SecurityContextHolder.getContext().getAuthentication();
		}
	}

	@Test
	@DisplayName("만료된 accessToken + 유효한 refreshToken 이 있으면 필터에서 토큰을 재발급하고 Authentication을 세팅한다")
	void reissue_tokens_when_accessToken_expired_and_refresh_token_valid()
		throws Exception {

		// given
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		// ActiveSession을 DB에 만들고 (sid/version 확정)
		ActiveSession session = activeSessionRepository.save(ActiveSession.create(user));
		String sid = session.getSessionId();
		long tokenVersion = session.getTokenVersion();

		// issueTokens가 Redis 저장 + DB 메타 저장까지 수행하는 구조(너 코드 기준)
		JwtDto tokens = authTokenService.issueTokens(user, sid, tokenVersion);

		// accessToken 만료 유도 (1초 설정이므로 1.1초 sleep)
		Thread.sleep(1100);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(
			new Cookie("accessToken", tokens.accessToken()),
			new Cookie("refreshToken", tokens.refreshToken())
		);

		MockHttpServletResponse response = new MockHttpServletResponse();
		CapturingChain chain = new CapturingChain();

		// when
		filter.doFilter(request, response, chain);

		// then: 체인에 도달했을 때 Authentication이 이미 세팅되어 있어야 함
		assertThat(chain.captured).isNotNull();
		assertThat(chain.captured.getPrincipal()).isInstanceOf(SecurityUser.class);

		SecurityUser securityUser = (SecurityUser)chain.captured.getPrincipal();
		assertThat(securityUser.getId()).isEqualTo(user.getId());

		// then: 응답 쿠키에 새 토큰이 내려왔는지 확인
		Cookie[] cookies = response.getCookies();
		assertThat(cookies).isNotEmpty();

		String newAccessToken = null;
		String newRefreshToken = null;
		for (Cookie cookie : cookies) {
			if ("accessToken".equals(cookie.getName())) {
				newAccessToken = cookie.getValue();
			}
			if ("refreshToken".equals(cookie.getName())) {
				newRefreshToken = cookie.getValue();
			}
		}

		assertThat(newAccessToken).isNotBlank();
		assertThat(newRefreshToken).isNotBlank();

		// access는 재발급되어야 하므로 기존과 달라야 함
		assertThat(newAccessToken).isNotEqualTo(tokens.accessToken());
	}
}
