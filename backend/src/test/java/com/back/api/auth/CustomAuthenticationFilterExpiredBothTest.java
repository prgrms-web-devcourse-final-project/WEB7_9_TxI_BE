package com.back.api.auth;

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

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.JwtProvider;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
	"custom.jwt.access-token-duration=0",
	"custom.jwt.refresh-token-duration=0"
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
	private JwtProvider jwtProvider;

	@Value("${custom.jwt.secret}")
	private String secret;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("accessToken, refreshToken 모두 만료되면 TOKEN_EXPIRED 에러가 발생한다")
	void token_expired_when_both_access_and_refresh_invalid() {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL);
		User user = testUser.user();

		String sid = "test-sid";
		long tokenVersion = 1L;

		String accessToken = jwtProvider.generateAccessToken(user, sid, tokenVersion);
		String refreshToken = jwtProvider.generateRefreshToken(user, sid, tokenVersion);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(
			new Cookie("accessToken", accessToken),
			new Cookie("refreshToken", refreshToken)
		);

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		org.assertj.core.api.Assertions.assertThatThrownBy(() ->
				filter.doFilter(request, response, chain)
			).isInstanceOf(ErrorException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.TOKEN_EXPIRED);
	}
}
