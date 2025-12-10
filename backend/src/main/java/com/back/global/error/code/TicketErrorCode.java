package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketErrorCode implements ErrorCode {

	TICKET_ALREADY_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이미 진행 중인 티켓이 존재합니다."),
	TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "티켓을 찾을 수 없습니다."),
	UNAUTHORIZED_TICKET_ACCESS(HttpStatus.FORBIDDEN, "티켓에 대한 접근 권한이 없습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
