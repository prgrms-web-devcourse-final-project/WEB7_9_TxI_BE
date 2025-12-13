package com.back.api.preregister.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "PreRegister API", description = "사전등록 API")
public interface PreRegisterApi {

	@Operation(
		summary = "사전등록 (V1 본인 인증)",
		description = "이벤트에 사전등록합니다. 이름, 비밀번호, 생년월일을 통한 본인 인증이 필요하며, 약관 동의가 필수입니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"NOT_FOUND_USER",
		"ALREADY_PRE_REGISTERED",
		"INVALID_PRE_REGISTRATION_PERIOD",
		"INVALID_USER_INFO",
		"TERMS_NOT_AGREED",
		"PRIVACY_NOT_AGREED"
	})
	ApiResponse<PreRegisterResponse> register(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId,
		@Valid @RequestBody PreRegisterCreateRequest request
	);

	@Operation(
		summary = "사전등록 취소",
		description = "사전등록을 취소합니다. 이미 취소된 경우 오류가 발생합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_PRE_REGISTER",
		"ALREADY_CANCELED"
	})
	ApiResponse<Void> cancel(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId
	);

	@Operation(
		summary = "내 사전등록 조회",
		description = "특정 이벤트에 대한 내 사전등록 정보를 조회합니다."
	)
	@ApiErrorCode("NOT_FOUND_PRE_REGISTER")
	ApiResponse<PreRegisterResponse> getMyPreRegister(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId
	);

	@Operation(
		summary = "사전등록 현황 조회",
		description = "특정 이벤트의 사전등록 수를 조회합니다."
	)
	@ApiErrorCode("NOT_FOUND_EVENT")
	ApiResponse<Long> getRegistrationCount(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "사전등록 여부 확인",
		description = "특정 이벤트에 사전등록했는지 여부를 확인합니다."
	)
	ApiResponse<Boolean> isRegistered(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId
	);
}
