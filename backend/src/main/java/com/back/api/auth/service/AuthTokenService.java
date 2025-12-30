package com.back.api.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.dto.cache.RefreshTokenCache;
import com.back.api.auth.store.AuthStore;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.RequestMetaProvider;
import com.back.global.security.JwtClaims;
import com.back.global.security.JwtProvider;
import com.back.global.utils.TokenHash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final RequestMetaProvider requestMetaProvider;
	private final SessionGuard sessionGuard;
	private final AuthStore authStore;

	@Transactional
	public JwtDto issueTokens(User user, String sessionId, long tokenVersion) {
		JwtDto dto = createJwtDto(user, sessionId, tokenVersion);

		saveRefreshMetaToDB(user, dto.refreshToken(), sessionId, tokenVersion);

		saveRefreshToStoreBestEffort(user.getId(), dto.refreshToken(), sessionId, tokenVersion);

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
		String jti = claims.jti();

		String hash = TokenHash.sha256(refreshTokenStr);

		// 캐시에 "현재 유효 refresh"가 들어있다면, 불일치 요청은 DB까지 가지 않고 차단
		if (tryEarlyRejectByCache(userId, sid, tokenVersion, hash)) {
			log.warn("ROTATE_EARLY_DENY(cache): userId={} sid={} tokenVersion={} jti={}",
				userId, sid, tokenVersion, jti);
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}

		// 1. 세션을 읽고 현재 세션과 비교
		ActiveSession active = sessionGuard.requireActiveSessionForUpdate(userId);
		sessionGuard.assertMatches(active, sid, tokenVersion);

		int updated = tokenRepository.revokeIfActive(hash);
		if (updated != 1) {
			log.warn("Rotate denied by concurrent/invalid refresh userId={} sid={} tokenVersion={}",
				userId, sid, tokenVersion);
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));

		try {
			JwtDto dto = issueTokens(user, active.getSessionId(), active.getTokenVersion());
			log.info("ROTATE_OK: userId={} sid={} tokenVersion={} oldJti={}", userId, sid, tokenVersion, jti);
			return dto;
		} catch (DataIntegrityViolationException e) {
			// partial unique index(유저당 active 1개) 위반 시그널
			log.error("ROTATE_CONFLICT: unique active refresh violated userId={} sid={} tokenVersion={} jti={}",
				userId, sid, tokenVersion, jti, e);
			throw new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE);
		}
	}

	private boolean tryEarlyRejectByCache(long userId, String sid, long tokenVersion, String refreshHash) {
		try {
			// 캐시가 아예 unavailable 이면(OPEN/timeout) -> DB로
			if (!authStore.isAvailable()) {
				return false;
			}

			return authStore.findRefreshCache(userId)
				.map(cache ->
					!refreshHash.equals(cache.getRefreshTokenHash())
						|| !sid.equals(cache.getSessionId())
						|| tokenVersion != cache.getTokenVersion())
				.orElse(false); // cache miss 면 DB로
		} catch (Exception e) {
			// Redis 장애는 rotate 자체를 막지 않고 DB로 진행 (가용성)
			log.warn("Redis cache check failed -> DB fallback. userId={}, cause={}", userId, e.toString());
			return false;
		}
	}

	private void saveRefreshToStoreBestEffort(long userId, String refreshToken, String sid, long tokenVersion) {
		if (!authStore.isAvailable()) {
			log.warn("RedisAuthStore unavailable: skip cache write userId={}", userId);
			return;
		}

		JwtClaims claims = jwtProvider.payloadOrNull(refreshToken);
		if (claims == null) {
			// 캐시에 잘못된 값 저장 방지
			log.warn("Refresh token claims null: skip cache write userId={}", userId);
			return;
		}

		long refreshValiditySeconds = jwtProvider.getRefreshTokenValiditySeconds();

		RefreshTokenCache cache = RefreshTokenCache.builder()
			.refreshTokenHash(TokenHash.sha256(refreshToken))
			.sessionId(sid)
			.tokenVersion(tokenVersion)
			.jti(claims.jti())
			.issuedAtEpochMs(System.currentTimeMillis())
			.build();

		try {
			authStore.saveRefreshCache(userId, cache, Duration.ofSeconds(refreshValiditySeconds));
		} catch (Exception e) {
			log.warn("Cache write failed (ignored). userId={}", userId, e);
		}
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
			.userAgent(requestMetaProvider.userAgent())
			.ipAddress(requestMetaProvider.clientIp())
			.sessionId(sid)
			.tokenVersion(tokenVersion)
			.build();

		tokenRepository.save(meta);
	}
}
