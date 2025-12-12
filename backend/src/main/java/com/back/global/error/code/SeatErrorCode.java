package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeatErrorCode implements ErrorCode {

	NOT_IN_QUEUE(HttpStatus.BAD_REQUEST, "큐에 입장하지 않은 사용자입니다."),
	NOT_FOUND_SEAT(HttpStatus.BAD_REQUEST, "해당 좌석을 찾을 수 없습니다."),
	NOT_FOUND_EVENT(HttpStatus.BAD_REQUEST, "이벤트를 찾을 수 없습니다."),

	SEAT_ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "선택할 수 없는 좌석입니다."),
	SEAT_ALREADY_SOLD(HttpStatus.BAD_REQUEST, "이미 판매된 좌석입니다."),
	SEAT_SELECTION_FAILED(HttpStatus.BAD_REQUEST, "좌석 선택에 실패했습니다."),
	SEAT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "좌석 상태 충돌이 발생했습니다. 다시 시도해주세요."),
	SEAT_CONCURRENCY_FAILURE(HttpStatus.BAD_REQUEST, "다른 사용자가 해당 좌석을 선택하는 중입니다. 다시 시도해주세요."),

	SEAT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "1개 이상 좌석을 선택할 수 없습니다."),

	// 관리자 예외
	DUPLICATE_SEAT_CODE(HttpStatus.BAD_REQUEST, "이미 존재하는 좌석 코드가 포함되어 있습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
