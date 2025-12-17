package com.back.global.services.sms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 인증번호 검증 요청")
public record SmsVerifyRequest(

	@Schema(description = "휴대폰 번호 (하이픈 제거)", example = "01012345678")
	@NotBlank(message = "휴대폰 번호는 필수입니다.")
	@Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
	String phoneNumber,

	@Schema(description = "인증번호 (6자리)", example = "123456")
	@NotBlank(message = "인증번호는 필수입니다.")
	@Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다.")
	@Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
	String verificationCode
) {
}
