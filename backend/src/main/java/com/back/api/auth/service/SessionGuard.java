package com.back.api.auth.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.cache.ActiveSessionDto;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionGuard {

	private final ActiveSessionRepository activeSessionRepository;
	private final ActiveSessionCache activeSessionCache;

	// ============================================================
	// Redis 캐싱 기반 메서드 (Access Token 검증용)
	// ============================================================

	/**
	 * ActiveSession 조회 및 검증 (Redis 우선, miss 시 DB 조회 후 캐싱)
	 * - Access Token 검증 시 사용
	 * - Redis 장애 시 fast-fail (TEMPORARY_AUTH_UNAVAILABLE)
	 */
	public void requireAndValidateSession(long userId, String sid, long tokenVersion) {
		ActiveSessionDto session = activeSessionCache.get(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		if (!session.matches(sid, tokenVersion)) {
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}
	}

	// ============================================================
	// 기존 DB 기반 메서드 (Refresh Token, 로그인 등에서 사용)
	// ============================================================

	@Transactional(readOnly = true)
	public ActiveSession requireActiveSession(long userId) {
		return activeSessionRepository.findByUserId(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
	}

	@Transactional
	public ActiveSession requireActiveSessionForUpdate(long userId) {
		return activeSessionRepository.findByUserIdForUpdate(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
	}

	public void assertMatches(ActiveSession active, String sid, long tokenVersion) {
		if (!active.getSessionId().equals(sid) || active.getTokenVersion() != tokenVersion) {
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}
	}
}
