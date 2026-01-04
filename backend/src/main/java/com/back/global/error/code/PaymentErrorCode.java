package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

	PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
	AMOUNT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "결제 금액 검증에 실패했습니다."),
	PAYMENT_KEY_MISMATCH(HttpStatus.BAD_REQUEST, "결제 키가 일치하지 않습니다."),
	PG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "결제 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."),
	PG_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "결제 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.");

	private final HttpStatus httpStatus;
	private final String message;
}
