package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QueueEntryErrorCode implements ErrorCode {

	NOT_FOUND_QUEUE_ENTRY(HttpStatus.NOT_FOUND, "큐 대기열 항목을 찾을 수 없습니다."),
	ALREADY_EXISTS_IN_QUEUE(HttpStatus.BAD_REQUEST, "이미 큐에 존재하는 항목입니다."),
	QUEUE_FULL(HttpStatus.BAD_REQUEST, "큐가 가득 찼습니다."),

	PRE_REGISTERED_USERS_EMPTY(HttpStatus.BAD_REQUEST, "사전 등록된 사용자가 없습니다."),
	QUEUE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 대기열이 존재합니다."),
	INVALID_PREREGISTER_LIST(HttpStatus.BAD_REQUEST, "유효하지 않은 사전 등록 사용자 목록입니다."),

	ALREADY_ENTERED(HttpStatus.BAD_REQUEST, "이미 입장하였습니다."),
	ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "이미 만료되었습니다."),
	NOT_WAITING_STATUS(HttpStatus.BAD_REQUEST, "대기중 상태가 아닙니다."),

	REDIS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결에 실패했습니다."),

	ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 결제가 완료되었습니다."),
	NOT_ENTERED_STATUS(HttpStatus.BAD_REQUEST, "입장 완료 상태가 아닙니다."),
	CANNOT_COMPLETE_PAYMENT(HttpStatus.BAD_REQUEST, "결제 완료 처리를 할 수 없는 상태입니다."),

	NOT_INVALID_COUNT(HttpStatus.BAD_REQUEST, "입장시킬 사용자가 없습니다.");
	private final HttpStatus httpStatus;
	private final String message;
}
