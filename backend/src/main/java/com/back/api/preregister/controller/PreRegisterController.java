package com.back.api.preregister.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.api.preregister.service.PreRegisterService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events/{eventId}/pre-registers")
@RequiredArgsConstructor
public class PreRegisterController implements PreRegisterApi {

	private final PreRegisterService preRegisterService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@PostMapping
	public ApiResponse<PreRegisterResponse> register(
		@PathVariable Long eventId,
		@Valid @RequestBody PreRegisterCreateRequest request) {
		Long userId = httpRequestContext.getUserId();
		PreRegisterResponse response = preRegisterService.register(eventId, userId, request);
		return ApiResponse.created("사전등록이 완료되었습니다.", response);
	}

	@Override
	@DeleteMapping
	public ApiResponse<Void> cancel(
		@PathVariable Long eventId) {
		Long userId = httpRequestContext.getUserId();
		preRegisterService.cancel(eventId, userId);
		return ApiResponse.noContent("사전등록이 취소되었습니다.");
	}

	@Override
	@GetMapping("/me")
	public ApiResponse<PreRegisterResponse> getMyPreRegister(
		@PathVariable Long eventId) {
		Long userId = httpRequestContext.getUserId();
		PreRegisterResponse response = preRegisterService.getMyPreRegister(eventId, userId);
		return ApiResponse.ok("사전등록 정보를 조회했습니다.", response);
	}

	@Override
	@GetMapping("/count")
	public ApiResponse<Long> getRegistrationCount(
		@PathVariable Long eventId) {
		Long count = preRegisterService.getRegistrationCount(eventId);
		return ApiResponse.ok("사전등록 현황을 조회했습니다.", count);
	}

	@Override
	@GetMapping("/status")
	public ApiResponse<Boolean> isRegistered(
		@PathVariable Long eventId) {
		Long userId = httpRequestContext.getUserId();
		Boolean isRegistered = preRegisterService.isRegistered(eventId, userId);
		return ApiResponse.ok("사전등록 여부를 확인했습니다.", isRegistered);
	}
}
