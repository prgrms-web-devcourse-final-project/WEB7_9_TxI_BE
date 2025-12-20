package com.back.global.logging;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
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
			MDC.clear(); // 중요: 스레드 재사용 대비
		}
	}
}