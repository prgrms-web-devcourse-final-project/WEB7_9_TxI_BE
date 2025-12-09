package com.back.api.auth.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.auth.dto.request.SignupRequest;
import com.back.api.auth.dto.response.AuthResponse;
import com.back.api.auth.service.AuthService;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth API", description = "회원 인증 API, 인증/인가(재발급)")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "사용자 회원가입", description = "이메일, 닉네임, 비밀번호, 생년월일로 회원가입")
	@PostMapping("/signup")
	public ApiResponse<AuthResponse> signup(
		@Valid @RequestBody SignupRequest request
	) {
		AuthResponse response = authService.signup(request);
		return ApiResponse.created("회원가입 성공", response);
	}
}
