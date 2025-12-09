package com.back.global.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.api.auth.service.AuthTokenService;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;
import com.back.global.properties.SiteProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
	private static final String BEARER_PREFIX = "Bearer ";

	private final HttpRequestContext requestContext;
	private final JwtProvider jwtProvider;
	private final AuthTokenService tokenService;
	private final SiteProperties siteProperties;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		addCorsHeaders(response);

		try {
			authenticate(request, response, filterChain);
		} catch (ErrorException e) {
			throw e;
		}
	}

	private void authenticate(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String requestUrl = request.getRequestURI();

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())
			|| !requestUrl.startsWith("/api/")
			|| requestUrl.startsWith(AUTH_PATH_PREFIX)
		) {
			filterChain.doFilter(request, response);
			return;
		}

		String headerAuthorization = requestContext.getHeader("Authorization", "");
		String accessToken = null;

		if (!headerAuthorization.isBlank() && headerAuthorization.startsWith(BEARER_PREFIX)) {
			accessToken = headerAuthorization.substring(BEARER_PREFIX.length());
		} else {
			accessToken = requestContext.getCookieValue("accessToken", "");
		}

		// TODO: 초기에 인증없이 모든 API 사용 가능하도록 설정, 기능 개발 완료 후 제거
		if (accessToken == null || accessToken.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		if (jwtProvider.isExpired(accessToken)) {
			throw new ErrorException(AuthErrorCode.TOKEN_EXPIRED);
		}

		Map<String, Object> payload = tokenService.payloadOrNull(accessToken);

		if (payload == null) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long userId = ((Number)payload.get("id")).longValue();
		String nickname = (String)payload.getOrDefault("nickname", "");
		UserRole role = (UserRole)payload.getOrDefault("role", UserRole.NORMAL);

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

	private void addCorsHeaders(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", siteProperties.getFrontUrl());
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
		response.setHeader("Access-Control-Max-Age", "3600");
	}
}
