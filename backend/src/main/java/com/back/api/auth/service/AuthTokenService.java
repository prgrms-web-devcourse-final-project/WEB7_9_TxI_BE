package com.back.api.auth.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.back.api.auth.dto.JwtDto;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.global.http.HttpRequestContext;
import com.back.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository tokenRepository;
	private final HttpRequestContext requestContext;

	public JwtDto generateTokens(User user) {
		String accessToken = jwtProvider.generateAccessToken(user);
		String refreshTokenStr = jwtProvider.generateRefreshToken(user);

		// === seconds 기준 ===
		long accessValiditySeconds = jwtProvider.getAccessTokenValiditySeconds();
		long refreshValiditySeconds = jwtProvider.getRefreshTokenValiditySeconds();

		// === 현재 시각 ===
		long nowEpochMillis = System.currentTimeMillis();
		LocalDateTime issuedAt = LocalDateTime.now();

		// === DB 저장용 만료 시각 (LocalDateTime) ===
		LocalDateTime expiresAt = issuedAt.plusSeconds(refreshValiditySeconds);

		String userAgent = requestContext.getUserAgent();
		String ip = requestContext.getClientIp();

		RefreshToken refreshToken = RefreshToken.builder()
			.user(user)
			.token(refreshTokenStr)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.revoked(false)
			.userAgent(userAgent)
			.ipAddress(ip)
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

	public Map<String, Object> payloadOrNull(String accessToken) {
		return jwtProvider.payloadOrNull(accessToken);
	}
}
