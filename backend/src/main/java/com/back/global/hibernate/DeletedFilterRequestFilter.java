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
		enabler.enable();

		filterChain.doFilter(request, response);
	}
}
