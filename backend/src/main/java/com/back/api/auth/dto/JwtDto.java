package com.back.api.auth.dto;

public record JwtDto(
	String tokenType,

	String accessToken,
	long accessTokenExpiresAt,
	long accessTokenExpiresIn,

	String refreshToken,
	long refreshTokenExpiresAt,
	long refreshTokenExpiresIn
) {
	public static final String BEARER = "Bearer";
}
