package com.back.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.ErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	// 커스텀 예외 처리
	@ExceptionHandler(ErrorException.class)
	protected ResponseEntity<ApiResponse<?>> handleCustomException(ErrorException ex) {
		ErrorCode code = ex.getErrorCode();

		// name() 대신 enum인지 체크 후 안전하게 로깅
		String codeName = (code instanceof Enum<?> e)
			? e.name()
			: code.getClass().getSimpleName();

		log.error("ErrorException: {} - {}", codeName, ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code.getHttpStatus(), ex.getMessage()));
	}

	// @Valid 유효성 검사 실패 시 발생하는 예외 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		ErrorCode code = CommonErrorCode.INVALID_INPUT_VALUE;

		// fieldError가 null일 수 있으므로 방어 코드
		String message = code.getMessage();
		if (ex.getBindingResult().getFieldError() != null) {
			String fieldMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
			if (fieldMessage != null && !fieldMessage.isBlank()) {
				message = fieldMessage;
			}
		}

		log.error("MethodArgumentNotValidException {}", ex.getMessage());

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code.getHttpStatus(), message));
	}

	// JSON 직렬화/역직렬화 예외
	@ExceptionHandler(JsonProcessingException.class)
	public ResponseEntity<ApiResponse<?>> handleJsonProcessing(JsonProcessingException ex) {
		ErrorCode code = CommonErrorCode.MESSAGE_NOT_READABLE;
		log.error("JSON parsing failed: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// @Validated 파라미터 검증 실패 (QueryParam, PathVariable 등)
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
		ErrorCode code = CommonErrorCode.INVALID_INPUT_VALUE;
		log.error("Constraint violation: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// 요청 Body 파싱 실패 (잘못된 JSON 구조 등)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<?>> handleNotReadable(HttpMessageNotReadableException ex) {
		ErrorCode code = CommonErrorCode.MESSAGE_NOT_READABLE;
		log.error("Message not readable: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// 타입 불일치 (ex: Long 자리에 문자열 등)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ErrorCode code = CommonErrorCode.TYPE_MISMATCH;
		log.error("Type mismatch: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// 필수 파라미터 누락
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException ex) {
		ErrorCode code = CommonErrorCode.MISSING_REQUEST_PARAMETER;
		log.error("Missing parameter: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// 허용되지 않은 HTTP Method
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		ErrorCode code = CommonErrorCode.METHOD_NOT_ALLOWED;
		log.error("Method not supported: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}

	// 비즈니스 로직 검증 실패 (IllegalArgumentException)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
		ErrorCode code = CommonErrorCode.INVALID_INPUT_VALUE;
		log.error("Illegal argument: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code.getHttpStatus(), ex.getMessage()));
	}

	// 그 외 모든 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleAllException(final Exception ex) {
		ErrorCode code = CommonErrorCode.INTERNAL_SERVER_ERROR;
		log.error("Unexpected exception: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(code.getHttpStatus())
			.body(ApiResponse.fail(code));
	}
}
