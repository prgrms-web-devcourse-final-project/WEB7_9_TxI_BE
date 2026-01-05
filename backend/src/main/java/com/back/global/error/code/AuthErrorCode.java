package com.back.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
	ALREADY_EXIST_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
	ALREADY_EXIST_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),

	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인 후 이용해주세요."),
	ACCESS_OTHER_DEVICE(HttpStatus.UNAUTHORIZED, "다른 환경에서 로그인했습니다."),
	TEMPORARY_AUTH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "인증 시스템 일시 장애입니다. 잠시 후 다시 시도해주세요."),

	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자 계정만 접근 가능합니다."),

	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "리프레시 토큰이 없습니다."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."),

	LOGIN_FAILED(HttpStatus.BAD_REQUEST, "이메일과 비밀번호가 올바른지 확인해주세요."),
	SOCIAL_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "로그인에 실패했습니다. 다시 시도해주세요."),

	PASSWORD_MISMATCH(HttpStatus.NOT_FOUND, "비밀번호가 일치하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
