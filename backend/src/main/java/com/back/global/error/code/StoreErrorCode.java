package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StoreErrorCode implements ErrorCode {
	NOT_FOUND(HttpStatus.NOT_FOUND, "상점을 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
