package com.back.api.event.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController implements EventApi {

	private final EventService eventService;

	@Override
	@PostMapping
	public ApiResponse<EventResponse> createEvent(
		@Valid @RequestBody EventCreateRequest request) {
		EventResponse response = eventService.createEvent(request);
		return ApiResponse.created("이벤트가 생성되었습니다.", response);
	}

	@Override
	@PutMapping("/{eventId}")
	public ApiResponse<EventResponse> updateEvent(
		@PathVariable Long eventId,
		@Valid @RequestBody EventUpdateRequest request) {
		EventResponse response = eventService.updateEvent(eventId, request);
		return ApiResponse.ok("이벤트가 수정되었습니다.", response);
	}

	@Override
	@DeleteMapping("/{eventId}")
	public ApiResponse<Void> deleteEvent(
		@PathVariable Long eventId) {
		eventService.deleteEvent(eventId);
		return ApiResponse.noContent("이벤트가 삭제되었습니다.");
	}

	@Override
	@GetMapping("/{eventId}")
	public ApiResponse<EventResponse> getEvent(
		@PathVariable Long eventId) {
		EventResponse response = eventService.getEvent(eventId);
		return ApiResponse.ok("이벤트를 조회했습니다.", response);
	}

	@Override
	@GetMapping
	public ApiResponse<Page<EventListResponse>> getEvents(
		@RequestParam(required = false) EventStatus status,
		@RequestParam(required = false) EventCategory category,
		@PageableDefault Pageable pageable) {
		Page<EventListResponse> response = eventService.getEvents(status, category, pageable);
		return ApiResponse.ok("이벤트 목록을 조회했습니다.", response);
	}
}
