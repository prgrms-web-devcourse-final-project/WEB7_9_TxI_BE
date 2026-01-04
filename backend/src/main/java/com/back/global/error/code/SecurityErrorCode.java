package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보안 관련 에러 코드
 *
 * Rate Limit, IDC IP 차단, Fingerprint 등 봇 차단 시스템 에러
 */
@Getter
@AllArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

	// Rate Limit
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	TOO_MANY_SMS_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 1분 후 다시 시도해주세요."),

	// IDC IP 차단
	IDC_IP_BLOCKED(HttpStatus.FORBIDDEN, "VPN 또는 프록시를 사용 중입니다. 해제 후 다시 시도해주세요."),

	// Device Fingerprint
	SUSPICIOUS_ACTIVITY(HttpStatus.BAD_REQUEST, "비정상적인 요청이 감지되었습니다. 잠시 후 다시 시도해주세요."),
	FINGERPRINT_BLOCKED(HttpStatus.FORBIDDEN, "비정상적인 활동이 감지되어 차단되었습니다. 고객센터에 문의해주세요.");

	private final HttpStatus httpStatus;
	private final String message;
}
