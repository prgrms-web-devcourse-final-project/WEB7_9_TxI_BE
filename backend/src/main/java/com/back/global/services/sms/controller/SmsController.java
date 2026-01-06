package com.back.global.services.sms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.global.response.ApiResponse;
import com.back.global.services.sms.dto.SmsSendRequest;
import com.back.global.services.sms.dto.SmsSendResponse;
import com.back.global.services.sms.dto.SmsVerifyRequest;
import com.back.global.services.sms.dto.SmsVerifyResponse;
import com.back.global.services.sms.service.SmsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController implements SmsApi {

	private final SmsService smsService;

	@Override
	@PostMapping("/send")
	public ApiResponse<SmsSendResponse> sendVerificationCode(
		@Valid @RequestBody SmsSendRequest request,
		@RequestHeader(value = "X-Device-Id", required = false) String visitorId
	) {
		Long expiresInSeconds = smsService.sendVerificationCode(request.phoneNumber(), request.eventId(), visitorId);
		return ApiResponse.ok("인증번호가 발송되었습니다.", SmsSendResponse.of(expiresInSeconds));
	}

	@Override
	@PostMapping("/verify")
	public ApiResponse<SmsVerifyResponse> verifyCode(
		@Valid @RequestBody SmsVerifyRequest request,
		@RequestHeader(value = "X-Device-Id", required = false) String visitorId
	) {
		smsService.verifyCode(request.phoneNumber(), request.verificationCode(), request.eventId(), visitorId);
		return ApiResponse.ok("인증에 성공하였습니다.", SmsVerifyResponse.of(true));
	}
}
