package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueEntryController implements QueueEntryApi {

	private final QueueEntryReadService queueEntryReadService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@GetMapping("/{eventId}/status")
	public ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUser().getId();
		QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);
		return ApiResponse.ok("대기열 상태를 조회했습니다.", response);

	}

	@Override
	@GetMapping("/{eventId}/exists")
	public ApiResponse<Boolean> existsInQueue(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUser().getId();
		boolean exists = queueEntryReadService.existsInWaitingQueue(eventId, userId);
		return ApiResponse.ok("대기열 진입 여부를 확인했습니다.", exists);
	}

}
