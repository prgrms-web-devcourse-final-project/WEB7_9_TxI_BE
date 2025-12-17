package com.back.global.services.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "SMS 인증번호 발송 요청")
public record SmsSendRequest(

	@Schema(description = "휴대폰 번호 (하이픈 제거)", example = "01012345678")
	@NotBlank(message = "휴대폰 번호는 필수입니다.")
	@Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
	String phoneNumber
) {
}
