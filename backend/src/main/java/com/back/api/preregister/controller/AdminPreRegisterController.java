package com.back.api.preregister.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.preregister.dto.response.PreRegisterListResponse;
import com.back.api.preregister.service.AdminPreRegisterService;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/pre-registers/{eventId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPreRegisterController implements AdminPreRegisterApi {

	private final AdminPreRegisterService adminPreRegisterService;

	@Override
	@GetMapping
	public ApiResponse<Page<PreRegisterListResponse>> getPreRegistersByEventId(
		@PathVariable Long eventId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<PreRegisterListResponse> response = adminPreRegisterService.getPreRegisterByEventId(
			eventId,
			page,
			size
		);

		return ApiResponse.ok("사전 등록 목록이 조회되었습니다.", response);
	}

	@Override
	@GetMapping("/count")
	public ApiResponse<Long> getPreRegisterCountByEventId(
		@PathVariable Long eventId
	) {
		Long count = adminPreRegisterService.getPreRegisterCountByEventId(eventId);
		return ApiResponse.ok("사전 등록 수가 조회되었습니다.", count);
	}

}
