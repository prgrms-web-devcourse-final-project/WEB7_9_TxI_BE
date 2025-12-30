package com.back.global.hibernate;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Soft-delete 필터 활성화 Filter
 * - 모든 HTTP 요청 시작 시 Hibernate deletedFilter 활성화
 * - ThreadLocal 기반 최적화로 요청당 한 번만 실행
 * - 요청 종료 시 ThreadLocal 정리 (메모리 누수 방지)
 */
@Component
@RequiredArgsConstructor
public class DeletedFilterRequestFilter extends OncePerRequestFilter {

	private final HibernateDeletedFilterEnabler enabler;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			// Hibernate deletedFilter 활성화
			enabler.enable();

			// 다음 필터 체인 실행
			filterChain.doFilter(request, response);
		} finally {
			// 메모리 누수방지 및 다음 요청에서 동일 Thread 재사용 시 잘못된 값 방지
			enabler.clear();
		}
	}
}
