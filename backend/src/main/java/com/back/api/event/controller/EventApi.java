package com.back.api.event.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Event API", description = "이벤트 CRUD 및 조회 API")
public interface EventApi {
	@Operation(
		summary = "이벤트 생성",
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
		summary = "이벤트 수정",
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
		summary = "이벤트 삭제",
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
		summary = "이벤트 단건 조회",
		description = "이벤트 ID로 상세 정보를 조회합니다. 이벤트 상세 페이지에서 사용됩니다."
	)
	@ApiErrorCode("NOT_FOUND_EVENT")
	ApiResponse<EventResponse> getEvent(
		@Parameter(description = "조회할 이벤트 ID", example = "1")
		@PathVariable Long eventId);

	@Operation(
		summary = "이벤트 목록 조회",
		description = "이벤트 목록을 조회합니다. 상태(status)와 카테고리(category)로 필터링할 수 있습니다."
	)
	ApiResponse<Page<EventListResponse>> getEvents(
		@Parameter(description = "이벤트 상태 필터 (미입력 시 전체 조회)", example = "PRE_OPEN")
		@RequestParam(required = false) EventStatus status,

		@Parameter(description = "이벤트 카테고리 필터 (미입력 시 전체 조회)", example = "CONCERT")
		@RequestParam(required = false) EventCategory category,

		@Parameter(description = "페이징 정보(기본값: page=0, size=10, sort=\"createAt\" 으로 설정해주세요.)")
		@PageableDefault(size = 10, sort = "createAt", direction = Sort.Direction.DESC)
		Pageable pageable);
}
