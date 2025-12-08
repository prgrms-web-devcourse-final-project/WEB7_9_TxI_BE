package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeatErrorCode implements ErrorCode {

	NOT_IN_QUEUE(HttpStatus.FORBIDDEN, "큐에 입장하지 않은 사용자입니다."),
	NOT_FOUND_SEAT(HttpStatus.NOT_FOUND, "해당 좌석을 찾을 수 없습니다."),
	NOT_FOUND_EVENT(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
	SEAT_ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "선택할 수 없는 좌석입니다."),
	SEAT_ALREADY_SOLD(HttpStatus.BAD_REQUEST, "이미 판매된 좌석입니다."),
	SEAT_SELECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "좌석 선택에 실패했습니다."),

	// 관리자 예외
	DUPLICATE_SEAT_CODE(HttpStatus.CONFLICT, "이미 존재하는 좌석 코드가 포함되어 있습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
