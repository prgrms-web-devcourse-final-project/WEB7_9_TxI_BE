package com.back.api.user.controller;

import com.back.api.user.dto.response.UserProfileResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "사용자 정보 조회 및 수정, 회원 탈퇴 API")
public interface UserApi {

	@Operation(
		summary = "사용자 정보 조회",
		description = "사용자 정보를 조회합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_USER",
	})
	ApiResponse<UserProfileResponse> getMe();
}
