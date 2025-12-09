package com.back.api.event.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.api.event.service.EventService;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Event API", description = "이벤트 CRUD 및 조회 API")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

	private final EventService eventService;

	@Operation(
		summary = "이벤트 생성",
		description = "새로운 이벤트를 생성합니다. 관리자 권한이 필요합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "이벤트 생성 성공",
			content = @Content(schema = @Schema(implementation = EventResponse.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (유효성 검사 실패)"
		)
	})
	@PostMapping
	public ApiResponse<EventResponse> createEvent(
		@Valid @RequestBody EventCreateRequest request) {
		EventResponse response = eventService.createEvent(request);
		return ApiResponse.created("이벤트가 생성되었습니다.", response);
	}

	@Operation(
		summary = "이벤트 수정",
		description = "기존 이벤트 정보를 수정합니다. 관리자 권한이 필요합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "이벤트 수정 성공",
			content = @Content(schema = @Schema(implementation = EventResponse.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "이벤트를 찾을 수 없음"
		)
	})
	@PutMapping("/{eventId}")
	public ApiResponse<EventResponse> updateEvent(
		@Parameter(description = "수정할 이벤트 ID", example = "1", required = true)
		@PathVariable Long eventId,
		@Valid @RequestBody EventUpdateRequest request) {
		EventResponse response = eventService.updateEvent(eventId, request);
		return ApiResponse.ok("이벤트가 수정되었습니다.", response);
	}

	@Operation(
		summary = "이벤트 삭제",
		description = "이벤트를 삭제합니다. 관리자 권한이 필요합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "204",
			description = "이벤트 삭제 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "이벤트를 찾을 수 없음"
		)
	})
	@DeleteMapping("/{eventId}")
	public ApiResponse<Void> deleteEvent(
		@Parameter(description = "삭제할 이벤트 ID", example = "1", required = true)
		@PathVariable Long eventId) {
		eventService.deleteEvent(eventId);
		return ApiResponse.noContent("이벤트가 삭제되었습니다.");
	}

	@Operation(
		summary = "이벤트 단건 조회",
		description = "이벤트 ID로 상세 정보를 조회합니다. "
			+ "이벤트 상세 페이지에서 사용됩니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "이벤트 조회 성공",
			content = @Content(schema = @Schema(implementation = EventResponse.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "이벤트를 찾을 수 없음"
		)
	})
	@GetMapping("/{eventId}")
	public ApiResponse<EventResponse> getEvent(
		@Parameter(description = "조회할 이벤트 ID", example = "1", required = true)
		@PathVariable Long eventId) {
		EventResponse response = eventService.getEvent(eventId);
		return ApiResponse.ok("이벤트를 조회했습니다.", response);
	}

	@Operation(
		summary = "이벤트 목록 조회",
		description = "이벤트 목록을 조회합니다. "
			+ "상태(status)와 카테고리(category)로 필터링할 수 있습니다. "
			+ "이벤트 목록 페이지에서 사용됩니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "이벤트 목록 조회 성공"
		)
	})
	@GetMapping
	public ApiResponse<Page<EventListResponse>> getEvents(
		@Parameter(
			description = "이벤트 상태 필터 (미입력 시 전체 조회)",
			example = "PRE_OPEN"
		)
		@RequestParam(required = false) EventStatus status,

		@Parameter(
			description = "이벤트 카테고리 필터 (미입력 시 전체 조회)",
			example = "CONCERT"
		)
		@RequestParam(required = false) EventCategory category,

		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@PageableDefault(size = 10, sort = "createAt", direction = Sort.Direction.DESC)
		Pageable pageable) {
		Page<EventListResponse> response = eventService.getEvents(status, category, pageable);
		return ApiResponse.ok("이벤트 목록을 조회했습니다.", response);
	}
}
