package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QueueEntryErrorCode implements ErrorCode {

	NOT_FOUND_QUEUE_ENTRY(HttpStatus.NOT_FOUND, "큐 대기열 항목을 찾을 수 없습니다."),
	ALREADY_EXISTS_IN_QUEUE(HttpStatus.BAD_REQUEST, "이미 큐에 존재하는 항목입니다."),
	QUEUE_FULL(HttpStatus.BAD_REQUEST, "큐가 가득 찼습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
