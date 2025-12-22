package com.back.api.event.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.AdminEventDashboardResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Event Admin API", description = "관리자용 이벤트 관리 및 현황 조회 API")
public interface AdminEventApi {

	@Operation(
		summary = "이벤트 생성 (관리자)",
		description = "새로운 이벤트를 생성합니다. 관리자 권한이 필요합니다."
	)
	@ApiErrorCode({
		"INVALID_EVENT_DATE",
		"DUPLICATE_EVENT",
		"EVENT_ALREADY_CLOSED",
		"PRE_REGISTER_NOT_OPEN"
	})
	ApiResponse<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request);

	@Operation(
		summary = "이벤트 수정 (관리자)",
		description = "기존 이벤트 정보를 수정합니다. 관리자 권한이 필요합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"INVALID_EVENT_DATE",
		"DUPLICATE_EVENT",
		"EVENT_ALREADY_CLOSED",
		"PRE_REGISTER_NOT_OPEN"
	})
	ApiResponse<EventResponse> updateEvent(
		@Parameter(description = "수정할 이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Valid @RequestBody EventUpdateRequest request);

	@Operation(
		summary = "이벤트 삭제 (관리자)",
		description = "이벤트를 삭제합니다. 관리자 권한이 필요합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"EVENT_ALREADY_CLOSED"
	})
	ApiResponse<Void> deleteEvent(
		@Parameter(description = "삭제할 이벤트 ID", example = "1")
		@PathVariable Long eventId);

	@Operation(
		summary = "전체 이벤트 대시보드 현황 조회 (관리자)",
		description = "관리자 대시보드에서 모든 이벤트의 전체 현황을 조회합니다. "
			+ "각 이벤트별 상태, 사전등록 인원 수, 총 판매 좌석, 총 판매 금액을 포함합니다."
	)
	ApiResponse<List<AdminEventDashboardResponse>> getAllEventsDashboard();
}
