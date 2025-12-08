package com.back.api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
	@Schema(description = "사용자 토큰 종류", example = "Bearer")
	String tokenType,

	@Schema(description = "사용자 엑세스 토큰")
	String accessToken,

	@Schema(description = "사용자 엑세스 토큰 만료시간")
	long accessTokenExpiresAt,

	@Schema(description = "사용자 리프레시 토큰")
	String refreshToken,

	@Schema(description = "사용자 리프레시 토큰 만료시간")
	long refreshTokenExpiresAt
) {
}
