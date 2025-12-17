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

	@Value("${custom.site.domain:localhost}")
	private String domain;

	@Value("${custom.jwt.access-token-duration}")
	private long accessTokenDurationMillis;

	@Value("${custom.jwt.refresh-token-duration}")
	private long refreshTokenDurationMillis;

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
		if (value == null) {
			value = "";
		}

		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setHttpOnly(true);

		boolean isLocalhostDomain = domain == null
			|| domain.isBlank()
			|| "localhost".equalsIgnoreCase(domain)
			|| "127.0.0.1".equals(domain);

		if (!isLocalhostDomain) {
			cookie.setDomain(domain);
		}

		boolean secureRequest = request.isSecure();
		// cookie.setSecure(secureRequest && !isLocalhostDomain); TODO 리팩토링
		cookie.setSecure(true);
		cookie.setAttribute("SameSite", "None");

		if (value.isBlank()) {
			cookie.setMaxAge(0);
		} else {
			if ("accessToken".equals(name)) {
				cookie.setMaxAge((int)accessTokenDurationMillis);
			} else if ("refreshToken".equals(name)) {
				cookie.setMaxAge((int)refreshTokenDurationMillis);
			}
		}

		response.addCookie(cookie);
	}

	public void deleteCookie(String name) {
		setCookie(name, null);
	}
}
