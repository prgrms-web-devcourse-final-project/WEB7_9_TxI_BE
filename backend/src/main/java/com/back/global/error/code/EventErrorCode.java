package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements ErrorCode {

	// ===== 이벤트 조회 실패 =====
	NOT_FOUND_EVENT(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),

	// ===== 이벤트 유효성 검증 실패 =====
	INVALID_EVENT_DATE(HttpStatus.BAD_REQUEST, "이벤트 날짜가 유효하지 않습니다."),
	EVENT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 마감된 이벤트입니다."),

	// ===== 사전등록 기간 관련 =====
	PRE_REGISTER_NOT_OPEN(HttpStatus.BAD_REQUEST, "사전등록 기간이 아닙니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
