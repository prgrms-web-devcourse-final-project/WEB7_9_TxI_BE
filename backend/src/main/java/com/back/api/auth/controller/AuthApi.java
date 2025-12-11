package com.back.api.auth.controller;

import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.auth.dto.request.LoginRequest;
import com.back.api.auth.dto.request.SignupRequest;
import com.back.api.auth.dto.response.AuthResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Auth API", description = "회원 인증 API, 인증/인가(재발급)")
public interface AuthApi {

	@Operation(summary = "사용자 회원가입", description = "이메일, 닉네임, 비밀번호, 생년월일로 회원가입")
	@ApiErrorCode({
		"ALREADY_EXIST_EMAIL",
		"ALREADY_EXIST_NICKNAME",
	})
	ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request);

	@Operation(summary = "사용자 로그인", description = "이메일, 비밀번호로 로그인")
	@ApiErrorCode({
		"LOGIN_FAILED",
	})
	ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request);

	@Operation(summary = "로그아웃", description = "쿠키에서 refresh 토큰을 제거하고 로그아웃")
	@ApiErrorCode({
		"REFRESH_TOKEN_REQUIRED",
		"REFRESH_TOKEN_NOT_FOUND",
		"UNAUTHORIZED"
	})
	ApiResponse<Void> logout();
}
