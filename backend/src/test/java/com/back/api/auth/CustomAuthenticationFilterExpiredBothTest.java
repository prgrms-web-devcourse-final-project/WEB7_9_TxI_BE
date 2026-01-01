package com.back.api.auth;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=1",
	"custom.jwt.refresh-token-duration=1"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class CustomAuthenticationFilterExpiredBothTest {

	@Autowired
	private CustomAuthenticationFilter filter;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private ActiveSessionRepository activeSessionRepository;

	@Autowired
	private AuthTokenService authTokenService;

	@Value("${custom.jwt.secret}")
	private String secret;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("accessToken, refreshToken 모두 만료되면 TOKEN_EXPIRED 에러가 발생한다")
	void token_expired_when_both_access_and_refresh_invalid() throws Exception {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL, null);
		User user = testUser.user();

		ActiveSession session = activeSessionRepository.save(ActiveSession.create(user));
		JwtDto tokens = authTokenService.issueTokens(user, session.getSessionId(), session.getTokenVersion());

		Thread.sleep(1100);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(
			new Cookie("accessToken", tokens.accessToken()),
			new Cookie("refreshToken", tokens.refreshToken())
		);

		MockHttpServletResponse response = new MockHttpServletResponse();
		// when
		filter.doFilter(request, response, new MockFilterChain());

		// then
		assertThat(response.getStatus()).isEqualTo(AuthErrorCode.TOKEN_EXPIRED.getHttpStatus().value());
		assertThat(response.getContentAsString()).contains(AuthErrorCode.TOKEN_EXPIRED.getMessage());
	}
}
