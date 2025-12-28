package com.back.global.services.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SMS 인증번호 발송 응답")
public record SmsSendResponse(
	@Schema(description = "인증번호 유효 시간(초)", example = "180")
	Long expiresInSeconds
) {
	public static SmsSendResponse of(Long expiresInSeconds) {
		return new SmsSendResponse(expiresInSeconds);
	}
}
