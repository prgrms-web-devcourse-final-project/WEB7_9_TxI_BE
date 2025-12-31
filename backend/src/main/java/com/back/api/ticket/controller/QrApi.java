package com.back.api.ticket.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.api.ticket.dto.response.QrTokenResponse;
import com.back.api.ticket.dto.response.QrValidationResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "QR API", description = "티켓 QR 발급 및 검증 API")
public interface QrApi {

	@Operation(
		summary = "QR 토큰 발급",
		description = "티켓에 대한 QR 토큰을 발급합니다. 이벤트 당일부터 발급이 됩니다."
	)
	@ApiErrorCode({
		"TICKET_NOT_FOUND",
		"UNAUTHORIZED_TICKET_ACCESS",
		"INVALID_TICKET_STATE",
		"EVENT_NOT_STARTED"
	})
	ApiResponse<QrTokenResponse> generateQrToken(
		@Parameter(description = "티켓 ID", example = "1")
		@PathVariable Long ticketId
	);

	@Operation(
		summary = "QR 코드 검증",
		description = "QR을 스캔하고 입장 가능 여부를 검증합니다."
	)
	@ApiErrorCode({
		"INVALID_QR_TOKEN",
		"QR_TOKEN_EXPIRED",
		"TICKET_NOT_FOUND"
	})
	ApiResponse<QrValidationResponse> validateQrCode(
		@Parameter(description = "QR 토큰", example = "abc123xyz456")
		@RequestParam String token
	);
}
