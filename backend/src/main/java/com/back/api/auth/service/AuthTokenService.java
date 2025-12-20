package com.back.api.auth.service;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;
import com.back.global.security.JwtClaims;
import com.back.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository tokenRepository;
	private final ActiveSessionRepository activeSessionRepository;
	private final UserRepository userRepository;
	private final HttpRequestContext requestContext;

	@Transactional
	public JwtDto issueTokens(User user, String sessionId, long tokenVersion) {
		String accessToken = jwtProvider.generateAccessToken(user, sessionId, tokenVersion);
		String refreshTokenStr = jwtProvider.generateRefreshToken(user, sessionId, tokenVersion);

		// === seconds 기준 ===
		long accessValiditySeconds = jwtProvider.getAccessTokenValiditySeconds();
		long refreshValiditySeconds = jwtProvider.getRefreshTokenValiditySeconds();

		// === 현재 시각 ===
		long nowEpochMillis = System.currentTimeMillis();
		LocalDateTime issuedAt = LocalDateTime.now();

		// === DB 저장용 만료 시각 (LocalDateTime) ===
		LocalDateTime expiresAt = issuedAt.plusSeconds(refreshValiditySeconds);

		RefreshToken refreshToken = RefreshToken.builder()
			.user(user)
			.token(refreshTokenStr)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.revoked(false)
			.userAgent(requestContext.getUserAgent())
			.ipAddress(requestContext.getClientIp())
			.sessionId(sessionId)
			.tokenVersion(tokenVersion)
			.build();

		tokenRepository.save(refreshToken);

		// === API 응답용 epoch millis ===
		long accessExpiresAtMillis = nowEpochMillis + (accessValiditySeconds * 1000);
		long refreshExpiresAtMillis = nowEpochMillis + (refreshValiditySeconds * 1000);

		return new JwtDto(
			JwtDto.BEARER,
			accessToken,
			accessExpiresAtMillis,
			accessValiditySeconds * 1000,
			refreshTokenStr,
			refreshExpiresAtMillis,
			refreshValiditySeconds * 1000
		);
	}

	@Transactional
	public JwtDto rotateTokenByRefreshToken(String refreshTokenStr) {
		if (StringUtils.isBlank(refreshTokenStr)) {
			throw new ErrorException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		if (jwtProvider.isExpired(refreshTokenStr)) {
			throw new ErrorException(AuthErrorCode.TOKEN_EXPIRED);
		}

		JwtClaims claims = jwtProvider.payloadOrNull(refreshTokenStr);

		if (claims == null || !"refresh".equals(claims.tokenType())) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long userId = claims.userId();
		String sid = claims.sessionId();
		long tokenVersion = claims.tokenVersion();

		ActiveSession active = activeSessionRepository.findByUserIdForUpdate(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		if (!active.getSessionId().equals(sid) || active.getTokenVersion() != tokenVersion) {
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}

		int revoked = tokenRepository.revokeIfActive(refreshTokenStr);
		if (revoked != 1) {
			throw new ErrorException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		return issueTokens(user, active.getSessionId(), active.getTokenVersion());
	}
}
