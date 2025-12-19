package com.back.api.preregister.controller;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "PreRegister API", description = "사전등록 API")
public interface PreRegisterApi {

	// 인증 있는 사전 등록 -> v2에서 사용 예정
	// @Operation(
	// 	summary = "사전등록",
	// 	description = "이벤트에 사전등록합니다. 휴대폰 번호, 생년월일을 통한 본인 인증이 필요하며, 약관 동의가 필수입니다. 이름은 JWT 토큰으로 인증된 사용자 정보에서 자동으로 가져옵니다.",
	// 	security = @SecurityRequirement(name = "bearerAuth")
	// )
	// @ApiErrorCode({
	// 	"NOT_FOUND_EVENT",
	// 	"NOT_FOUND_USER",
	// 	"ALREADY_PRE_REGISTERED",
	// 	"INVALID_PRE_REGISTRATION_PERIOD",
	// 	"INVALID_USER_INFO",
	// 	"TERMS_NOT_AGREED",
	// 	"PRIVACY_NOT_AGREED",
	// 	"UNAUTHORIZED"
	// })
	// ApiResponse<PreRegisterResponse> register(
	// 	@Parameter(description = "이벤트 ID", example = "1")
	// 	@PathVariable Long eventId,
	// 	@Valid @RequestBody PreRegisterCreateRequest request
	// );

	@Operation(
		summary = "사전등록",
		description = "이벤트에 사전등록합니다. (인증 제외)",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"NOT_FOUND_USER",
		"ALREADY_PRE_REGISTERED",
		"INVALID_PRE_REGISTRATION_PERIOD",
		"INVALID_USER_INFO",
		"TERMS_NOT_AGREED",
		"PRIVACY_NOT_AGREED",
		"UNAUTHORIZED"
	})
	ApiResponse<PreRegisterResponse> register(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "사전등록 취소",
		description = "사전등록을 취소합니다. 이미 취소된 경우 오류가 발생합니다. JWT 토큰을 통해 사용자를 인증합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_PRE_REGISTER",
		"ALREADY_CANCELED",
		"UNAUTHORIZED"
	})
	ApiResponse<Void> cancel(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "내 사전등록 조회",
		description = "특정 이벤트에 대한 내 사전등록 정보를 조회합니다. JWT 토큰을 통해 사용자를 인증합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode({
		"NOT_FOUND_PRE_REGISTER",
		"UNAUTHORIZED"
	})
	ApiResponse<PreRegisterResponse> getMyPreRegister(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
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
		description = "특정 이벤트에 사전등록했는지 여부를 확인합니다. JWT 토큰을 통해 사용자를 인증합니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiErrorCode("UNAUTHORIZED")
	ApiResponse<Boolean> isRegistered(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
