package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SmsErrorCode implements ErrorCode {

	// ===== SMS 발송 실패 =====
	SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호 형식입니다."),
	SMS_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "SMS 발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),

	// ===== SMS 인증 실패 =====
	VERIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 존재하지 않습니다. 인증번호를 다시 요청해주세요."),
	VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),

	// ===== SMS 인증 상태 오류 =====
	SMS_VERIFICATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "휴대폰 인증을 완료해주세요."),
	SMS_VERIFICATION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 인증이 완료된 전화번호입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
