package com.back.api.event.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.back.api.event.dto.response.AdminEventDashboardResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.api.event.service.AdminEventService;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController implements AdminEventApi {

	private final AdminEventService adminEventService;

	@Override
	@PostMapping
	public ApiResponse<EventResponse> createEvent(
		@Valid @RequestBody EventCreateRequest request) {
		EventResponse response = adminEventService.createEvent(request);
		return ApiResponse.created("이벤트가 생성되었습니다.", response);
	}

	@Override
	@PutMapping("/{eventId}")
	public ApiResponse<EventResponse> updateEvent(
		@PathVariable Long eventId,
		@Valid @RequestBody EventUpdateRequest request) {
		EventResponse response = adminEventService.updateEvent(eventId, request);
		return ApiResponse.ok("이벤트가 수정되었습니다.", response);
	}

	@Override
	@DeleteMapping("/{eventId}")
	public ApiResponse<Void> deleteEvent(
		@PathVariable Long eventId) {
		adminEventService.deleteEvent(eventId);
		return ApiResponse.noContent("이벤트가 삭제되었습니다.");
	}

	@Override
	@GetMapping("/dashboard")
	public ApiResponse<Page<AdminEventDashboardResponse>> getAllEventsDashboard(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<AdminEventDashboardResponse> responses = adminEventService.getAllEventsDashboard(
			page,
			size
		);
		return ApiResponse.ok("이벤트 현황 조회 성공", responses);
	}

	@Override
	@GetMapping("/{eventId}")
	public ApiResponse<EventResponse> getEvent(
		@PathVariable Long eventId) {
		EventResponse response = adminEventService.getEventForAdmin(eventId);
		return ApiResponse.ok("이벤트를 조회했습니다.", response);
	}

}
