package com.back.api.preregister.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.api.preregister.dto.response.PreRegisterListResponse;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "PreRegister Admin API", description = "관리자용 사전등록 API")
public interface AdminPreRegisterApi {

	@Operation(summary = "사전 등록 목록 조회", description = "특정 이벤트의 전체 사전 등록 목록을 페이징하여 조회합니다.")
	ApiResponse<Page<PreRegisterListResponse>> getPreRegistersByEventId(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@Parameter(description = "페이지 번호 (0부터 시작)")
		@RequestParam(defaultValue = "0") int page,

		@Parameter(description = "페이지 크기")
		@RequestParam(defaultValue = "20") int size
	);

	@Operation(summary = "사전 등록 총 수 조회", description = "특정 이벤트의 전체 사전 등록 수를 조회합니다.")
	ApiResponse<Long> getPreRegisterCountByEventId(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
