package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "QueueEntry API", description = "사용자 대기열 API")
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueEntryController {

	private final QueueEntryReadService queueEntryReadService;

	@GetMapping("/{eventId}/status")
	@Operation(summary = "내 대기열 상태 조회", description = "사용자의 현재 대기열 상태를 조회합니다.")
	public ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId
	) {
		QueueEntryStatusResponse response = queueEntryReadService.getMyQueueStatus(eventId, userId);
		return ApiResponse.ok("대기열 상태를 조회했습니다.", response);

	}


	@GetMapping("/{eventId}/exists")
	@Operation(summary = "대기열 진입 여부 조회", description = "사용자가 특정 이벤트의 대기열에 진입했는지 조회합니다.")
	public ApiResponse<Boolean> existsInQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@Parameter(description = "사용자 ID", example = "1")
		@RequestParam Long userId
	) {
		boolean exists = queueEntryReadService.existsInWaitingQueue(eventId, userId);
		return ApiResponse.ok("대기열 진입 여부를 확인했습니다.", exists);
	}

}
