package com.back.api.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.user.dto.request.UpdateProfileRequest;
import com.back.api.user.dto.response.UserProfileResponse;
import com.back.api.user.service.UserService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('NORMAL')")
public class UserController implements UserApi {

	private final UserService userService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@GetMapping("/profile")
	public ApiResponse<UserProfileResponse> getMe() {
		long userId = httpRequestContext.getUserId();
		UserProfileResponse response = userService.getUser(userId);
		return ApiResponse.ok(String.format("%s 사용자 조회 성공", userId), response);
	}

	@Override
	@PutMapping("/profile")
	public ApiResponse<UserProfileResponse> updateProfile(
		@Validated @RequestBody UpdateProfileRequest request
	) {
		long userId = httpRequestContext.getUserId();
		UserProfileResponse response = userService.updateProfile(userId, request);
		return ApiResponse.ok(String.format("%s 사용자 정보 변경 완료", userId), response);
	}

	@Override
	@DeleteMapping("/me")
	public ApiResponse<Void> deleteUser() {
		long userUd = httpRequestContext.getUserId();
		userService.deleteUser(userUd);
		return ApiResponse.noContent("회원탈퇴 완료");
	}
}
