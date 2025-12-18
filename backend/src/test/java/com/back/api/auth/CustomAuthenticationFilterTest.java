package com.back.api.auth;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.JwtProvider;
import com.back.global.security.SecurityUser;
import com.back.global.utils.JwtUtil;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CustomAuthenticationFilterTest {

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
	@DisplayName("만료된 accessToken + 유효한 refreshToken 이 있으면 필터에서 토큰을 재발급하고 Authentication을 세팅한다")
	void reissueTokensWhenAccessTokenExpiredAndRefreshTokenValid() throws ServletException, IOException {
		// given
		TestUser testUser = userHelper.createUser(UserRole.NORMAL);
		User user = testUser.user();

		Map<String, Object> payload = Map.of(
			"id", user.getId(),
			"nickname", user.getNickname(),
			"role", user.getRole().name()
		);

		// 이미 만료된 accessToken (유효기간을 음수로 줘서 발급 시점부터 만료 상태)
		String expiredAccessToken = JwtUtil.toString(secret, -1L, payload);

		// 아직 유효한 refreshToken
		String validRefreshToken = JwtUtil.toString(
			secret,
			jwtProvider.getRefreshTokenValiditySeconds(),
			payload
		);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(
			new Cookie("accessToken", expiredAccessToken),
			new Cookie("refreshToken", validRefreshToken)
		);

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		// when
		filter.doFilter(request, response, chain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isInstanceOf(SecurityUser.class);

		SecurityUser securityUser = (SecurityUser)authentication.getPrincipal();
		assertThat(securityUser.getId()).isEqualTo(user.getId());

		// 응답에 새 토큰이 쿠키로 세팅되었는지 확인
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
		assertThat(newAccessToken).isNotEqualTo(expiredAccessToken);
	}

	@Test
	@DisplayName("accessToken, refreshToken 모두 만료되면 TOKEN_EXPIRED 에러가 발생한다")
	void tokenExpiredWhenBothAccessAndRefreshInvalid() {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL);
		User user = testUser.user();

		Map<String, Object> payload = Map.of(
			"id", user.getId(),
			"nickname", user.getNickname(),
			"role", user.getRole()
		);

		String expiredAccessToken = JwtUtil.toString(secret, -1L, payload);
		String expiredRefreshToken = JwtUtil.toString(secret, -1L, payload);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/some-resource");
		request.setCookies(
			new Cookie("accessToken", expiredAccessToken),
			new Cookie("refreshToken", expiredRefreshToken)
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
