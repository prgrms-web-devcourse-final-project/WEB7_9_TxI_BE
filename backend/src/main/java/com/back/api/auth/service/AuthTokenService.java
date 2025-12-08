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

		long nowEpochMillis = System.currentTimeMillis();
		long refreshValidityMillis = jwtProvider.getRefreshTokenValidityMillis();
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusNanos(refreshValidityMillis * 1_000_000L);

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

		long accessValidityMillis = jwtProvider.getAccessTokenValidityMillis();

		return new JwtDto(
			JwtDto.BEARER,
			accessToken,
			nowEpochMillis + accessValidityMillis,
			accessValidityMillis,
			refreshTokenStr,
			nowEpochMillis + refreshValidityMillis,
			refreshValidityMillis
		);
	}

	public Map<String, Object> payloadOrNull(String accessToken) {
		return jwtProvider.payloadOrNull(accessToken);
	}
}
