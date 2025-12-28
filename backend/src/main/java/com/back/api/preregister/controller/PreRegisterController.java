package com.back.api.preregister.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.api.preregister.service.PreRegisterService;
import com.back.global.http.HttpRequestContext;
import com.back.global.recaptcha.service.ReCaptchaService;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PreRegisterController implements PreRegisterApi {

	private final PreRegisterService preRegisterService;
	private final HttpRequestContext httpRequestContext;
	private final ReCaptchaService reCaptchaService;

	@Override
	@PostMapping("/events/{eventId}/pre-registers")
	public ApiResponse<PreRegisterResponse> register(
		@PathVariable Long eventId,
		@RequestHeader(value = "X-Recaptcha-Token", required = false) String recaptchaToken,
		@Valid @RequestBody PreRegisterCreateRequest request) {
		reCaptchaService.verifyToken(recaptchaToken, null);
		Long userId = httpRequestContext.getUserId();
		PreRegisterResponse response = preRegisterService.register(eventId, userId, request);
		return ApiResponse.created("사전등록이 완료되었습니다.", response);
	}

	@Override
	@DeleteMapping("/events/{eventId}/pre-registers")
	public ApiResponse<Void> cancel(
		@PathVariable Long eventId) {
		Long userId = httpRequestContext.getUserId();
		preRegisterService.cancel(eventId, userId);
		return ApiResponse.noContent("사전등록이 취소되었습니다.");
	}

	/**
	 * 내 사전등록 정보 다건 조회
	 */
	@Override
	@GetMapping("/pre-registers/me")
	public ApiResponse<List<PreRegisterResponse>> getMyPreRegister() {
		Long userId = httpRequestContext.getUserId();
		List<PreRegisterResponse> response = preRegisterService.getMyPreRegister(userId);
		return ApiResponse.ok("사전등록 정보를 조회했습니다.", response);
	}

	@Override
	@GetMapping("/events/{eventId}/pre-registers/count")
	public ApiResponse<Long> getRegistrationCount(
		@PathVariable Long eventId) {
		Long count = preRegisterService.getRegistrationCount(eventId);
		return ApiResponse.ok("사전등록 현황을 조회했습니다.", count);
	}

	@Override
	@GetMapping("/events/{eventId}/pre-registers/status")
	public ApiResponse<Boolean> isRegistered(
		@PathVariable Long eventId) {
		Long userId = httpRequestContext.getUserId();
		Boolean isRegistered = preRegisterService.isRegistered(eventId, userId);
		return ApiResponse.ok("사전등록 여부를 확인했습니다.", isRegistered);
	}
}

