package com.back.api.auth.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.auth.dto.request.LoginRequest;
import com.back.api.auth.dto.request.SignupRequest;
import com.back.api.auth.dto.request.VerifyPasswordRequest;
import com.back.api.auth.dto.response.AuthResponse;
import com.back.api.auth.service.AuthService;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController implements AuthApi {

	private final AuthService authService;

	@Override
	@PostMapping("/signup")
	public ApiResponse<AuthResponse> signup(
		@Valid @RequestBody SignupRequest request
	) {
		AuthResponse response = authService.signup(request);
		return ApiResponse.created("회원가입 성공", response);
	}

	@Override
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(
		@Valid @RequestBody LoginRequest request
	) {
		AuthResponse response = authService.login(request);
		return ApiResponse.created("로그인 성공", response);
	}

	@Override
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		authService.logout();
		return ApiResponse.noContent("로그아웃 되었습니다.");
	}

	@Override
	@PostMapping("/verify-password")
	public ApiResponse<Void> verifyPassword(@Valid @RequestBody VerifyPasswordRequest request) {
		authService.verifyPassword(request.password());
		return ApiResponse.noContent("비밀번호 인증 완료");
	}
}
