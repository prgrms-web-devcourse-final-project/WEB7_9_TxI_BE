package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
	// ===== 사용자 관련 =====
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),

	// ===== 요청 유효성 관련 =====
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 타입이 올바르지 않습니다."),
	MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),

	// ===== HTTP METHOD 오류 =====
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

	// ===== 서버 내부 오류 =====
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
