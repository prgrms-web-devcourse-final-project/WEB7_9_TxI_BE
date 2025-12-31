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
	TICKET_EVENT_MISMATCH(HttpStatus.BAD_REQUEST, "티켓과 이벤트가 일치하지 않습니다."),
	TICKET_QR_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "티켓 QR 토큰이 만료되었습니다."),
	INVALID_TICKET_QR_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 티켓 QR 토큰입니다."),
	TICKET_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 티켓입니다."),
	EVENT_NOT_STARTED(HttpStatus.BAD_REQUEST, "이벤트 시작 전에는 QR이 발급되지 않습니다."),
	TICKET_ALREADY_ENTERED(HttpStatus.BAD_REQUEST, "이미 입장처 티켓입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
