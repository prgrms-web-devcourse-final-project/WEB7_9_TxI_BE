package com.back.global.logging;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @Component등록 없이 SecurityConfig에서 명시적으로 빈으로 등록 후 활용
 * Security FilterChain에서 확실하게 순서보장 목적
 */
public class RequestIdFilter extends OncePerRequestFilter {

	private static final String HEADER = "X-Request-Id";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String requestId = Optional.ofNullable(request.getHeader(HEADER))
			.filter(id -> !id.isBlank())
			.orElse(UUID.randomUUID().toString());

		MDC.put("requestId", requestId);
		response.setHeader(HEADER, requestId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("requestId");
		}
	}
}