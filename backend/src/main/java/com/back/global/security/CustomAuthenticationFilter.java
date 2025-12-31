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
import com.back.api.auth.service.SessionGuard;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.ErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.CookieManager;
import com.back.global.logging.MdcContext;
import com.back.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFilter extends OncePerRequestFilter {
	private static final String BEARER_PREFIX = "Bearer ";

	private static final Set<String> AUTH_WHITELIST = Set.of(
		"/api/v1/auth/login",
		"/api/v1/auth/signup"
	);

	private static final Set<String> PATH_PREFIX_WHITELIST = Set.of(
		"/api/v1/events"
	);

	private final JwtProvider jwtProvider;
	private final AuthTokenService tokenService;
	private final CookieManager cookieManager;
	private final SessionGuard sessionGuard;
	private final ObjectMapper objectMapper;

	@Value("${custom.jwt.access-token-duration:3600}")
	private long accessTokenDurationSeconds;

	@Value("${custom.jwt.refresh-token-duration:1209600}")
	private long refreshTokenDurationSeconds;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws IOException {
		try {
			authenticate(request, response, filterChain);
		} catch (ErrorException e) {
			handleErrorException(request, response, e);
		} catch (Exception e) {
			log.error("Unexpected auth filter error: ", e);
			writeError(response, AuthErrorCode.UNAUTHORIZED);
		} finally {
			SecurityContextHolder.clearContext();
			MdcContext.removeUserId();
		}
	}

	private void authenticate(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String requestUrl = request.getRequestURI();

		// bypass
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())
			|| !requestUrl.startsWith("/api/")
			|| AUTH_WHITELIST.contains(requestUrl)
			|| isWhitelistedPath(requestUrl)
		) {
			filterChain.doFilter(request, response);
			return;
		}

		String accessToken = resolveAccessToken(request);

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

		// ActiveSession Redis 캐싱 적용
		try {
			sessionGuard.requireAndValidateSession(userId, sid, tokenVersion);
		} catch (ErrorException e) {
			if (e.getErrorCode() == AuthErrorCode.ACCESS_OTHER_DEVICE) {
				cookieManager.deleteAuthCookies(request, response);
			}
			throw e;
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

	private boolean isWhitelistedPath(String requestUrl) {
		for (String prefix : PATH_PREFIX_WHITELIST) {
			if (requestUrl.startsWith(prefix)) {
				// Whitelist 방식: 명시적으로 허용할 패턴만 정의 (Default Deny)

				// 1. 이벤트 목록 조회: /api/v1/events (쿼리 파라미터 허용)
				if (requestUrl.matches("^/api/v1/events(\\?.*)?$")) {
					return true;
				}

				// 2. 이벤트 상세 조회: /api/v1/events/{id}
				if (requestUrl.matches("^/api/v1/events/\\d+$")) {
					return true;
				}

				// 3. 사전등록 수 조회: /api/v1/events/{id}/pre-registers/count
				if (requestUrl.matches("^/api/v1/events/\\d+/pre-registers/count$")) {
					return true;
				}

				// 위 패턴에 해당하지 않으면 인증 필요
				return false;
			}
		}
		return false;
	}

	private void handleErrorException(
		HttpServletRequest request,
		HttpServletResponse response,
		ErrorException error
	) throws IOException {
		if (response.isCommitted()) {
			return;
		}

		if (error.getErrorCode() == AuthErrorCode.ACCESS_OTHER_DEVICE
			|| error.getErrorCode() == AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
			|| error.getErrorCode() == AuthErrorCode.TOKEN_EXPIRED
		) {
			cookieManager.deleteAuthCookies(request, response);
		}

		log.error("CustomAuthenticationFilter: ", error);
		writeError(response, error.getErrorCode());
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String headerAuthorization = request.getHeader("Authorization");
		if (!StringUtils.isBlank(headerAuthorization)
			&& headerAuthorization.startsWith(BEARER_PREFIX)
		) {
			String value = headerAuthorization.substring(BEARER_PREFIX.length());
			return StringUtils.isBlank(value) ? null : value;
		}
		return resolveCookie(request, "accessToken");
	}

	private String resolveCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				String value = cookie.getValue();
				return StringUtils.isBlank(value) ? null : value;
			}
		}

		return null;
	}

	private String ensureValidAccessToken(
		String accessToken,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		if (StringUtils.isBlank(accessToken)) {
			throw new ErrorException(AuthErrorCode.UNAUTHORIZED);
		}

		if (!jwtProvider.isExpired(accessToken)) {
			return accessToken;
		}

		log.info("Expired Access Token → Try to Issue new Access Token, expired access token: {}",
			accessToken);
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

	private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
		response.setStatus(code.getHttpStatus().value());
		response.setContentType("application/json; charset=UTF-8");

		ApiResponse<?> body = ApiResponse.fail(code);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
