package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

	// 404 NOT_FOUND (자원을 찾을 수 없을 때)
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 알림을 찾을 수 없습니다."),

	// 403 FORBIDDEN (인가되지 않은 접근 시도 - 다른 사용자 알림 접근)
	NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),

	// 400 BAD_REQUEST (잘못된 요청 데이터)
	INVALID_NOTIFICATION_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 알림 ID 형식입니다."),

	// 500 INTERNAL_SERVER_ERROR (서버 내부 에러)
	NOTIFICATION_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 처리 중 서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
