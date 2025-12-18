package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.queue.dto.request.ShuffleQueueRequest;
import com.back.api.queue.dto.response.CompletedQueueResponse;
import com.back.api.queue.dto.response.QueueStatisticsResponse;
import com.back.api.queue.dto.response.ShuffleQueueResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "QueueEntry Admin API", description = "관리자 대기열 API")
public interface AdminQueueEntryApi {

	@Operation(
		summary = "대기열 셔플",
		description = "이벤트의 대기열을 랜덤 큐로 셔플합니다.(수동)"
	)
	@ApiErrorCode({
		"EVENT_NOT_FOUND",
		"PRE_REGISTERED_USERS_EMPTY",
		"QUEUE_ALREADY_EXISTS",
		"INVALID_PREREGISTER_LIST",
		"QUEUE_READY",
		"REDIS_CONNECTION_FAILED"
	})
	ApiResponse<ShuffleQueueResponse> shuffleQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@RequestBody @Valid ShuffleQueueRequest request
	);

	@Operation(
		summary = "대기열 통계 조회",
		description = "이벤트의 대기열 통계를 조회합니다."
	)
	@ApiErrorCode("NOT_FOUND_QUEUE_ENTRY")
	ApiResponse<QueueStatisticsResponse> getQueueStatistics(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "결제 완료 처리",
		description = "특정 사용자의 결제를 완료 처리합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_QUEUE_ENTRY",
		"ALREADY_COMPLETED",
		"ALREADY_EXPIRED",
		"NOT_ENTERED_STATUS"
	})
	ApiResponse<CompletedQueueResponse> completePayment(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@Parameter(description = "사용자 ID", example = "1")
		@PathVariable Long userId
	);

	@Operation(
		summary = "[테스트용] 대기열 초기화",
		description = "특정 이벤트의 대기열(REDIS)을 완전히 초기화합니다."
	)
	ApiResponse<Void> resetQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

}
