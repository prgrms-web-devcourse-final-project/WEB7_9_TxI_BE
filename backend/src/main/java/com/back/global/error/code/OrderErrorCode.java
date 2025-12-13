package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {

	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 내역을 찾을 수 없습니다."),
	TICKET_EVENT_MISMATCH(HttpStatus.BAD_REQUEST, "티켓과 이벤트가 일치하지 않습니다."),
	AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "주문 금액이 올바르지 않습니다."),
	UNAUTHORIZED_ORDER_ACCESS(HttpStatus.BAD_REQUEST, "해당 주문에 접근할 권한이 없습니다."),
	INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "주문 상태가 유효하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
