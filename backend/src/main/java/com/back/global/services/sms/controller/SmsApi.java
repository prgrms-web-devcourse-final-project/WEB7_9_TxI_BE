package com.back.global.services.sms.controller;

import org.springframework.web.bind.annotation.RequestBody;

import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;
import com.back.global.services.sms.dto.SmsSendRequest;
import com.back.global.services.sms.dto.SmsVerifyRequest;
import com.back.global.services.sms.dto.SmsVerifyResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "SMS API", description = "SMS 인증번호 발송 및 검증 API")
public interface SmsApi {
	@Operation(
		summary = "SMS 인증번호 발송",
		description = "휴대폰 번호로 6자리 인증번호를 발송합니다. 인증번호는 3분간 유효합니다."
	)
	@ApiErrorCode({
		"INVALID_PHONE_NUMBER",
		"SMS_SEND_FAILED",
		"SMS_SEND_LIMIT_EXCEEDED"
	})
	ApiResponse<Void> sendVerificationCode(@Valid @RequestBody SmsSendRequest request);

	@Operation(
		summary = "SMS 인증번호 검증",
		description = "발송된 인증번호를 검증합니다. 인증 성공 시 10분간 사전등록이 가능합니다."
	)
	@ApiErrorCode({
		"VERIFICATION_CODE_NOT_FOUND",
		"VERIFICATION_CODE_MISMATCH",
		"VERIFICATION_CODE_EXPIRED",
		"SMS_VERIFICATION_NOT_COMPLETED",
		"SMS_VERIFICATION_ALREADY_COMPLETED"
	})
	ApiResponse<SmsVerifyResponse> verifyCode(@Valid @RequestBody SmsVerifyRequest request);
}
