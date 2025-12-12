package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PreRegisterErrorCode implements ErrorCode {

	// ===== 사전등록 조회 실패 =====
	NOT_FOUND_PRE_REGISTER(HttpStatus.NOT_FOUND, "사전등록 내역을 찾을 수 없습니다."),

	// ===== 사전등록 유효성 검증 실패 =====
	ALREADY_PRE_REGISTERED(HttpStatus.BAD_REQUEST, "이미 사전등록되어 있습니다."),
	INVALID_USER_INFO(HttpStatus.BAD_REQUEST, "입력한 정보가 회원 정보와 일치하지 않습니다."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
	INVALID_PRE_REGISTRATION_PERIOD(HttpStatus.BAD_REQUEST, "사전등록 기간이 아닙니다."),

	// ===== 약관 동의 오류 =====
	TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "이용약관에 동의해야 합니다."),
	PRIVACY_NOT_AGREED(HttpStatus.BAD_REQUEST, "개인정보 수집 및 이용에 동의해야 합니다."),

	// ===== 사전등록 상태 오류 =====
	ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 사전등록입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
