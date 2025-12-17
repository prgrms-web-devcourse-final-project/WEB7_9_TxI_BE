package com.back.global.services.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SMS 인증번호 검증 응답")
public record SmsVerifyResponse(

	@Schema(description = "인증 성공 여부", example = "true")
	boolean verified
) {
	public static SmsVerifyResponse of(boolean verified) {
		return new SmsVerifyResponse(verified);
	}
}
