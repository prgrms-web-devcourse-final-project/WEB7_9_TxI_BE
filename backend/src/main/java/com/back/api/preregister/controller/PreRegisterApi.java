package com.back.api.preregister.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "PreRegister API", description = "사전등록 API")
public interface PreRegisterApi {

	@Operation(
		summary = "사전등록",
		description = "이벤트 사전등록 API입니다. 휴대폰 번호와 생년월일을 통한 본인 인증 및 "
			+ "약관 동의가 필요합니다. reCAPTCHA v3 토큰은 "
			+ "헤더(X-Recaptcha-Token)로 전달해야 합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"NOT_FOUND_USER",
		"ALREADY_PRE_REGISTERED",
		"INVALID_PRE_REGISTRATION_PERIOD",
		"INVALID_USER_INFO",
		"TERMS_NOT_AGREED",
		"PRIVACY_NOT_AGREED",
		"RECAPTCHA_TOKEN_MISSING",
		"RECAPTCHA_VERIFICATION_FAILED",
		"RECAPTCHA_SCORE_TOO_LOW",
		"UNAUTHORIZED"
	})
	ApiResponse<PreRegisterResponse> register(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "reCAPTCHA v3 토큰", example = "03AGdBq24...")
		@RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
		@Parameter(description = "디바이스 ID (Fingerprint)", example = "abcdef1234567890")
		@RequestHeader(value = "X-Device-Id", required = false) String deviceId,
		@Valid @RequestBody PreRegisterCreateRequest request,
		jakarta.servlet.http.HttpServletRequest httpRequest
	);

	@Operation(
		summary = "사전등록 취소",
		description = "사전등록을 취소합니다. 이미 취소된 경우 오류가 발생합니다. JWT 토큰을 통해 사용자를 인증합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_PRE_REGISTER",
		"ALREADY_CANCELED",
		"UNAUTHORIZED"
	})
	ApiResponse<Void> cancel(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "내 사전 등록 리스트 조회",
		description = "특정 이벤트에 대한 내 사전등록 리스트 정보를 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_PRE_REGISTER",
		"UNAUTHORIZED"
	})
	ApiResponse<List<PreRegisterResponse>> getMyPreRegister();

	@Operation(
		summary = "현재 사전등록 인원 수 조회",
		description = "특정 이벤트의 현재 등록한 사전등록 인원 현황을 조회합니다."
	)
	@ApiErrorCode("NOT_FOUND_EVENT")
	ApiResponse<Long> getRegistrationCount(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "내 사전등록 신청여부",
		description = "사용자가 특정 이벤트에 사전등록했는지 여부를 확인합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode("UNAUTHORIZED")
	ApiResponse<Boolean> isRegistered(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}

