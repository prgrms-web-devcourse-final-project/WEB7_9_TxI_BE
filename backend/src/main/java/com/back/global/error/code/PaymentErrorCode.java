package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

	PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
