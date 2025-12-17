package com.back.global.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.CookieManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
	private static final String BEARER_PREFIX = "Bearer ";

	private static final Set<String> AUTH_WHITELIST = Set.of(
		"/api/v1/auth/login",
		"/api/v1/auth/signup"
	);

	private final JwtProvider jwtProvider;
	private final AuthTokenService tokenService;
	private final UserRepository userRepository;
	private final CookieManager cookieManager;

	@Value("${jwt.access-token-duration:3600}")
	private long accessTokenDurationSeconds;

	@Value("${jwt.refresh-token-duration:1209600}")
	private long refreshTokenDurationSeconds;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		authenticate(request, response, filterChain);
	}

	private void authenticate(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String requestUrl = request.getRequestURI();

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())
			|| !requestUrl.startsWith("/api/")
			|| AUTH_WHITELIST.contains(requestUrl)
		) {
			filterChain.doFilter(request, response);
			return;
		}

		String accessToken = resolveAccessToken(request);

		// TODO: 초기에 인증없이 모든 API 사용 가능하도록 설정, 기능 개발 완료 후 제거
		if (accessToken == null || accessToken.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		String validAccessToken = ensureValidAccessToken(accessToken, request, response);

		Map<String, Object> payload = tokenService.payloadOrNull(validAccessToken);
		if (payload == null) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long userId = ((Number)payload.get("id")).longValue();
		String nickname = (String)payload.getOrDefault("nickname", "");
		Object roleObj = payload.get("role");
		UserRole role = UserRole.NORMAL;

		if (roleObj instanceof String roleStr) {
			role = UserRole.valueOf(roleStr);
		} else if (roleObj instanceof UserRole userRole) {
			role = userRole;
		}

		SecurityUser securityUser = new SecurityUser(
			userId,
			"",
			nickname,
			role,
			List.of(new SimpleGrantedAuthority(role.toAuthority()))
		);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			securityUser,
			null,
			securityUser.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String headerAuthorization = request.getHeader("Authorization");
		if (headerAuthorization != null
			&& !headerAuthorization.isBlank()
			&& headerAuthorization.startsWith(BEARER_PREFIX)
		) {
			return headerAuthorization.substring(BEARER_PREFIX.length());
		}
		return resolveCookie(request, "accessToken");
	}

	private String resolveCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return "";
		}

		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return "";
	}

	private String ensureValidAccessToken(
		String accessToken,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		if (!jwtProvider.isExpired(accessToken)) {
			return accessToken;
		}

		String refreshToken = resolveCookie(request, CookieManager.REFRESH_TOKEN_COOKIE);

		if (refreshToken == null || refreshToken.isBlank() || jwtProvider.isExpired(refreshToken)) {
			// refreshToken도 없거나 만료 > 세션 종료 > 재로그인 필요
			throw new ErrorException(AuthErrorCode.TOKEN_EXPIRED);
		}

		Map<String, Object> refreshPayload = jwtProvider.payloadOrNull(refreshToken);
		if (refreshPayload == null) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long userIdFromRefresh = ((Number)refreshPayload.get("id")).longValue();

		User user = userRepository.findById(userIdFromRefresh)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		JwtDto newTokens = tokenService.generateTokens(user);

		cookieManager.set(request, response, "accessToken", newTokens.accessToken(), accessTokenDurationSeconds);
		cookieManager.set(request, response, "refreshToken", newTokens.refreshToken(), refreshTokenDurationSeconds);

		return newTokens.accessToken();
	}
}
