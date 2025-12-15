package com.back.api.user.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.user.dto.request.UpdateProfileRequest;
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
		"NOT_FOUND",
	})
	ApiResponse<UserProfileResponse> getMe();

	@Operation(summary = "개인 프로필 정보 수정", description = "개인 프로필 정보 수정")
	@ApiErrorCode({
		"ALREADY_EXIST_NICKNAME",
		"NOT_FOUND"
	})
	ApiResponse<UserProfileResponse> updateProfile(
		@Validated @RequestBody UpdateProfileRequest request
	);

	@Operation(
		summary = "사용자 회원탈퇴",
		description = "회원탈퇴 시 호출하는 API 입니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_USER",
	})
	ApiResponse<Void> deleteUser();
}
