package com.back.global.error.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.back.global.error.exception.ErrorException;
import com.back.global.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	// 커스텀 예외 처리
	@ExceptionHandler(ErrorException.class)
	protected ResponseEntity<ApiResponse<?>> handleCustomException(ErrorException exception) {
		log.error("ErrorException: {} - {}", exception.getErrorCode().name(), exception.getMessage(), exception);
		return new ResponseEntity<>(
			ApiResponse.fail(exception), exception.getErrorCode().getHttpStatus());
	}

	// @Valid 유효성 검사 실패 시 발생하는 예외 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {

		String message = exception.getBindingResult().getFieldError().getDefaultMessage();

		log.warn("MethodArgumentNotValidException {}", exception.getMessage());
		return new ResponseEntity<>(
			ApiResponse.fail(HttpStatus.BAD_REQUEST, message), HttpStatus.BAD_REQUEST);
	}

	// JSON 직렬화/역직렬화 예외
	@ExceptionHandler(JsonProcessingException.class)
	public ResponseEntity<ApiResponse<?>> handleJsonProcessing(JsonProcessingException exception) {
		log.error("JSON 파싱 실패: {}", exception.getMessage());
		return new ResponseEntity<>(
			ApiResponse.fail(HttpStatus.BAD_REQUEST, exception.getMessage()), HttpStatus.BAD_REQUEST);
	}

	// 그 외 모든 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleAllException(final Exception exception) {
		log.error("handleAllException {}", exception.getMessage(), exception);
		return new ResponseEntity<>(
			ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage()),
			HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
