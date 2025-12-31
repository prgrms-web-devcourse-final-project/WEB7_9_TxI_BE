package com.back.api.ticket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.ticket.dto.response.QrTokenResponse;
import com.back.api.ticket.dto.response.QrValidationResponse;
import com.back.api.ticket.service.QrService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class QrController implements QrApi {

	private final QrService qrService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@PostMapping("/{ticketId}/qr-token")
	public ApiResponse<QrTokenResponse> generateQrToken(
		@PathVariable Long ticketId
	){
		Long userId = httpRequestContext.getUserId();
		QrTokenResponse response = qrService.generateQrTokenResponse(ticketId, userId);

		return ApiResponse.ok("QR 토큰 발급 성공", response);
	}

	@Override
	@GetMapping("/entry/verify")
	public ApiResponse<QrValidationResponse> validateQrCode(
		@RequestParam String token
	){
		Long userId = httpRequestContext.getUserId();
		QrValidationResponse response = qrService.validateAndProcessEntry(token);

		return ApiResponse.ok("QR 코드 검증 성공", response);
	}
}
