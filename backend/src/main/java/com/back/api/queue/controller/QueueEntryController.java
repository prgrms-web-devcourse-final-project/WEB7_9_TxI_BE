package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.request.ProcessEntriesRequest;
import com.back.api.queue.dto.response.ProcessEntriesResponse;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueEntryController implements QueueEntryApi {

	private final QueueEntryReadService queueEntryReadService;
	private final HttpRequestContext httpRequestContext;
	private final QueueEntryProcessService queueEntryProcessService;

	@Override
	@GetMapping("/{eventId}/status")
	public ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUserId();
		QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);
		return ApiResponse.ok("대기열 상태를 조회했습니다.", response);

	}

	@Override
	@GetMapping("/{eventId}/exists")
	public ApiResponse<Boolean> existsInQueue(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUserId();
		boolean exists = queueEntryReadService.existsInWaitingQueue(eventId, userId);
		return ApiResponse.ok("대기열 진입 여부를 확인했습니다.", exists);
	}

	@Override
	@PostMapping("/{eventId}/process-entries")
	public ApiResponse<ProcessEntriesResponse> processTopEntries(
		@PathVariable Long eventId,
		@RequestBody(required = false) @Valid ProcessEntriesRequest request
	) {
		Long userId = httpRequestContext.getUserId();
		int entryCount = (request != null) ? request.getCountOrDefault() : 1;

		ProcessEntriesResponse response = queueEntryProcessService.processTopEntriesForTest(
			eventId,
			entryCount
		);

		return ApiResponse.ok("입장 처리가 완료되었습니다.", response);
	}

	@Override
	@PostMapping("/{eventId}/process-until-me")
	public ApiResponse<ProcessEntriesResponse> processUntilMe(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUserId();

		ProcessEntriesResponse response = queueEntryProcessService.processTopEntriesUntilMeForTest(
			eventId,
			userId
		);

		return ApiResponse.ok("내 앞 사용들이 모두 입장 처리가 완료되었습니다.", response);
	}

	@Override
	@PostMapping("/{eventId}/process-include-me")
	public ApiResponse<ProcessEntriesResponse> processIncludingMe(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUserId();

		ProcessEntriesResponse response = queueEntryProcessService.processTopEntriesIncludingMeForTest(
			eventId,
			userId
		);

		return ApiResponse.ok("나를 포함한 내 앞 사용들이 모두 입장 처리가 완료되었습니다.", response);
	}

}
