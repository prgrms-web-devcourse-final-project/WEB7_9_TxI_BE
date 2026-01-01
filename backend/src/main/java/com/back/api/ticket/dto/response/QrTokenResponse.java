package com.back.api.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QR 토큰 응답 DTO")
public record QrTokenResponse(
	@Schema(description = "QR에 포함될 JWT 토큰", example = "abc123xyz456")
	String qrToken,

	@Schema(description = "토큰 만료 시간(초)", example = "60")
	int expirationSecond,

	@Schema(description = "QR 코드 갱신 간격(초)", example = "30")
	int refreshIntervalSecond,

	@Schema(description = "QR 코드 URL", example = "https://www.waitfair.com/tickets/verifiy?token=abc123xyz456")
	String qrUrl

) {
}
