package com.back.api.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.dto.cache.RefreshTokenCache;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.RefreshTokenRedisRepository;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;
import com.back.global.security.JwtClaims;
import com.back.global.security.JwtProvider;
import com.back.global.utils.TokenHash;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final HttpRequestContext requestContext;
	private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	private final SessionGuard sessionGuard;

	@Transactional
	public JwtDto issueTokens(User user, String sessionId, long tokenVersion) {
		JwtDto dto = createJwtDto(user, sessionId, tokenVersion);

		saveRefreshToRedis(user.getId(), dto.refreshToken(), sessionId, tokenVersion);

		saveRefreshMetaToDB(user, dto.refreshToken(), sessionId, tokenVersion);

		return dto;
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

		// 1. 세션을 읽고 현재 세션과 비교
		ActiveSession active = sessionGuard.requireActiveSessionForUpdate(userId);

		sessionGuard.assertMatches(active, sid, tokenVersion);

		// 2. Redis 검증: 현재 유효 refresh 인지 확인 (해시 비교)
		RefreshTokenCache cached = refreshTokenRedisRepository.find(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE));

		String hash = TokenHash.sha256(refreshTokenStr);
		if (!hash.equals(cached.getRefreshTokenHash())
			|| !sid.equals(cached.getSessionId())
			|| tokenVersion != cached.getTokenVersion()) {
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}

		int updated = tokenRepository.revokeIfActive(hash);
		if (updated != 1) {
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		return issueTokens(user, active.getSessionId(), active.getTokenVersion());
	}

	private JwtDto createJwtDto(User user, String sessionId, long tokenVersion) {
		String accessToken = jwtProvider.generateAccessToken(user, sessionId, tokenVersion);
		String refreshToken = jwtProvider.generateRefreshToken(user, sessionId, tokenVersion);

		// === seconds 기준 ===
		long accessValiditySeconds = jwtProvider.getAccessTokenValiditySeconds();
		long refreshValiditySeconds = jwtProvider.getRefreshTokenValiditySeconds();

		// === 현재 시각 ===
		long nowEpochMillis = System.currentTimeMillis();

		// === API 응답용 epoch millis ===
		long accessExpiresAtMillis = nowEpochMillis + (accessValiditySeconds * 1000);
		long refreshExpiresAtMillis = nowEpochMillis + (refreshValiditySeconds * 1000);

		return new JwtDto(
			JwtDto.BEARER,
			accessToken,
			accessExpiresAtMillis,
			accessValiditySeconds * 1000,
			refreshToken,
			refreshExpiresAtMillis,
			refreshValiditySeconds * 1000
		);
	}

	private void saveRefreshToRedis(long userId, String refreshToken, String sid, long tokenVersion) {
		JwtClaims claims = jwtProvider.payloadOrNull(refreshToken);
		if (claims == null) {
			throw new ErrorException(AuthErrorCode.INVALID_TOKEN);
		}

		long refreshValiditySeconds = jwtProvider.getRefreshTokenValiditySeconds();

		RefreshTokenCache cache = RefreshTokenCache.builder()
			.refreshTokenHash(TokenHash.sha256(refreshToken))
			.sessionId(sid)
			.tokenVersion(tokenVersion)
			.jti(claims.jti())
			.issuedAtEpochMs(System.currentTimeMillis())
			.build();

		refreshTokenRedisRepository.save(userId, cache, Duration.ofSeconds(refreshValiditySeconds));
	}

	private void saveRefreshMetaToDB(User user, String refreshToken, String sid, long tokenVersion) {
		JwtClaims claims = jwtProvider.payloadOrNull(refreshToken);
		String jti = (claims == null) ? null : claims.jti();

		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());

		RefreshToken meta = RefreshToken.builder()
			.user(user)
			.token(TokenHash.sha256(refreshToken))
			.jti(jti)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.revoked(false)
			.userAgent(requestContext.getUserAgent())
			.ipAddress(requestContext.getClientIp())
			.sessionId(sid)
			.tokenVersion(tokenVersion)
			.build();

		tokenRepository.save(meta);
	}
}
