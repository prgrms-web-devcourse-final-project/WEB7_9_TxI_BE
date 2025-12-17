package com.back.global.http;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.security.SecurityUser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HttpRequestContext {

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final UserRepository userRepository;
	private final CookieManager cookieManager;

	@Value("${custom.jwt.access-token-duration}")
	private long accessTokenDurationSeconds;

	@Value("${custom.jwt.refresh-token-duration}")
	private long refreshTokenDurationSeconds;

	private Authentication getAuthentication() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
	}

	public SecurityUser getSecurityUser() {
		return Optional.of(getAuthentication())
			.map(Authentication::getPrincipal)
			.filter(principal -> principal instanceof SecurityUser)
			.map(principal -> (SecurityUser)principal)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
	}

	public User getUser() {
		SecurityUser securityUser = getSecurityUser();

		return userRepository.findById(securityUser.getId())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
	}

	public Long getUserId() {
		return getSecurityUser().getId();
	}

	public String getHeader(String name, String defaultValue) {
		return Optional
			.ofNullable(request.getHeader(name))
			.filter(value -> !value.isBlank())
			.orElse(defaultValue);
	}

	public String getUserAgent() {
		String userAgent = request.getHeader("User-Agent");
		return userAgent != null ? userAgent : "unknown";
	}

	public String getClientIp() {
		String ip = request.getHeader("X-Forwarded-For");

		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			return ip.split(",")[0].trim();
		}

		ip = request.getHeader("X-Real-IP");
		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			return ip;
		}

		return request.getRemoteAddr();
	}

	public String getCookieValue(String name, String defaultValue) {
		return Optional
			.ofNullable(request.getCookies())
			.flatMap(
				cookies -> Arrays.stream(cookies)
					.filter(cookie -> cookie.getName().equals(name))
					.map(Cookie::getValue)
					.filter(value -> !value.isBlank())
					.findFirst()
			)
			.orElse(defaultValue);
	}

	public void setCookie(String name, String value) {
		long maxAgeSec = 0;

		if (value != null && !value.isBlank()) {
			if ("accessToken".equals(name)) {
				maxAgeSec = accessTokenDurationSeconds;
			} else if ("refreshToken".equals(name)) {
				maxAgeSec = refreshTokenDurationSeconds;
			}
		}

		cookieManager.set(request, response, name, value, maxAgeSec);
	}

	public void deleteCookie(String name) {
		cookieManager.delete(request, response, name);
	}

	public void setAccessTokenCookie(String token) {
		cookieManager.setAccessToken(request, response, token, accessTokenDurationSeconds);
	}

	public void setRefreshTokenCookie(String token) {
		cookieManager.setRefreshToken(request, response, token, refreshTokenDurationSeconds);
	}

	public void deleteAccessTokenCookie() {
		cookieManager.deleteAccessToken(request, response);
	}

	public void deleteRefreshTokenCookie() {
		cookieManager.deleteRefreshToken(request, response);
	}

	public void deleteAuthCookies() {
		cookieManager.deleteAuthCookies(request, response);
	}
}
