package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketErrorCode implements ErrorCode {

	TICKET_ALREADY_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이미 진행 중인 티켓이 존재합니다."),
	TICKET_NOT_FOUND(HttpStatus.BAD_REQUEST, "티켓을 찾을 수 없습니다."),
	UNAUTHORIZED_TICKET_ACCESS(HttpStatus.BAD_REQUEST, "티켓에 대한 접근 권한이 없습니다."),
	INVALID_TICKET_STATE(HttpStatus.BAD_REQUEST, "티켓 상태가 유효하지 않습니다."),
	SEAT_ALREADY_PURCHASED(HttpStatus.BAD_REQUEST, "해당 좌석은 이미 구매된 상태입니다."),
	TICKET_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중인 티켓이 아닙니다."),
	TICKET_EVENT_MISMATCH(HttpStatus.BAD_REQUEST, "티켓과 이벤트가 일치하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
