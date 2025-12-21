package com.back.global.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.service.AuthTokenService;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.CookieManager;
import com.back.global.logging.MdcContext;

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
	private final CookieManager cookieManager;
	private final ActiveSessionRepository activeSessionRepository;

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

		JwtClaims claims = jwtProvider.payloadOrNull(validAccessToken);
		if (claims == null || !"access".equals(claims.tokenType())) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long userId = claims.userId();
		String nickname = claims.nickname();
		UserRole role = claims.role();
		String sid = claims.sessionId();
		long tokenVersion = claims.tokenVersion();

		ActiveSession active = activeSessionRepository.findByUserId(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		if (!active.getSessionId().equals(sid) || active.getTokenVersion() != tokenVersion) {
			cookieManager.deleteAuthCookies(request, response);
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
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

		// 인증 성공 이후에 MDC 로그 컨텍스트에 userId추가, SecurityContext와 별개
		MdcContext.putUserId(userId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MdcContext.removeUserId(); // 다음 스레드 재사용을 위해 반드시 제거
		}
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String headerAuthorization = request.getHeader("Authorization");
		if (!StringUtils.isBlank(headerAuthorization)
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

		String refreshTokenStr = resolveCookie(request, CookieManager.REFRESH_TOKEN_COOKIE);

		if (StringUtils.isBlank(refreshTokenStr) || jwtProvider.isExpired(refreshTokenStr)) {
			// refreshToken도 없거나 만료 > 세션 종료 > 재로그인 필요
			throw new ErrorException(AuthErrorCode.TOKEN_EXPIRED);
		}

		try {
			JwtDto newTokens = tokenService.rotateTokenByRefreshToken(refreshTokenStr);

			cookieManager.set(request, response, "accessToken", newTokens.accessToken(), accessTokenDurationSeconds);
			cookieManager.set(request, response, "refreshToken", newTokens.refreshToken(), refreshTokenDurationSeconds);

			return newTokens.accessToken();
		} catch (ErrorException e) {
			// 다른 기기 로그인/세션 불일치인 경우 쿠키 삭제
			if (e.getErrorCode() == AuthErrorCode.ACCESS_OTHER_DEVICE
				|| e.getErrorCode() == AuthErrorCode.REFRESH_TOKEN_NOT_FOUND) {
				cookieManager.deleteAuthCookies(request, response);
			}
			throw e;
		}
	}
}
