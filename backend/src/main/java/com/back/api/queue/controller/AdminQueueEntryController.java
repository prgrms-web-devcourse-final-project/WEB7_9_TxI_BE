package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.dto.request.ShuffleQueueRequest;
import com.back.api.queue.dto.response.QueueStatisticsResponse;
import com.back.api.queue.dto.response.ShuffleQueueResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.queue.service.QueueShuffleService;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "QueueEntry Admin API", description = "관리자 대기열 API")
@RestController
@RequestMapping("/api/v1/admin/queues")
@RequiredArgsConstructor
public class AdminQueueEntryController {

	private final QueueShuffleService queueShuffleService;
	private final QueueEntryReadService queueEntryReadService;
	private final QueueEntryProcessService queueEntryProcessService;

	@PostMapping("/{eventId}/shuffle")
	@Operation(summary = "대기열 셔플", description = "이벤트의 대기열을 랜덤 큐로 셔플합니다.(수동)")
	public ApiResponse<ShuffleQueueResponse> shuffleQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@RequestBody @Valid ShuffleQueueRequest request
	) {
		queueShuffleService.shuffleQueue(eventId, request.preRegisteredUserIds());

		ShuffleQueueResponse response = ShuffleQueueResponse.from(
			eventId,
			request.preRegisteredUserIds().size()
		);
		return ApiResponse.created("랜덤 큐가 생성되었습니다.", response);
	}


	@GetMapping("/{eventId}/statistics")
	@Operation(summary = "대기열 통계 조회", description = "이벤트의 대기열 통계를 조회합니다.")
	public ApiResponse<QueueStatisticsResponse> getQueueStatistics(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	) {
		QueueStatisticsResponse response = queueEntryReadService.getQueueStatistics(eventId);
		return ApiResponse.ok("대기열 통계를 조회했습니다.", response);
	}

	//테스트용
	@PostMapping("/{eventId}/users/{userId}/complete")
	@Operation(summary = "결제 완료 처리", description = "특정 사용자의 결제를 완료 처리합니다.")
	public ApiResponse<Void> completePayment(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@Parameter(description = "사용자 ID", example = "1")
		@PathVariable Long userId
	) {

		queueEntryProcessService.completePayment(eventId, userId);
		return ApiResponse.noContent("결제 완료 처리되었습니다.");

	}
}
