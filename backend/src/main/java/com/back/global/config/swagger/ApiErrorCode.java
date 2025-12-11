package com.back.global.config.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.back.global.error.code.ErrorCode;

/**
 * API에서 발생 가능한 에러 코드를 명시하는 어노테이션
 *
 * 사용 예시:
 * - 단일 에러: @ApiErrorCode("NOT_FOUND_EVENT")
 * - 여러 에러: @ApiErrorCode({"NOT_FOUND_EVENT", "DUPLICATE_SEAT_CODE"})
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCode {
	/**
	 * 발생 가능한 ErrorCode enum 상수 이름 목록
	 */
	String[] value();
}
