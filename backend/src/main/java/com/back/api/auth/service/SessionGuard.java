package com.back.api.auth.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionGuard {

	private final ActiveSessionRepository activeSessionRepository;

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
