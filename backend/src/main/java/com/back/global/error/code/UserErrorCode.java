package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
	NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	CAN_NOT_DELETE_USER(HttpStatus.BAD_REQUEST, "신청하신 행사가 종료된 후에 탈퇴 할 수 있습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
