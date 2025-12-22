package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.queue.dto.request.ProcessEntriesRequest;
import com.back.api.queue.dto.response.ProcessEntriesResponse;
import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "QueueEntry API", description = "사용자 대기열 API")
public interface QueueEntryApi {

	@Operation(
		summary = "내 대기열 상태 조회",
		description = "사용자의 현재 대기열 상태를 조회합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_QUEUE_ENTRY",
		"NOT_WAITING_STATUS"
	})
	ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);


	@Operation(
		summary = "대기열 진입 여부 조회",
		description = "사용자가 특정 이벤트의 대기열에 진입했는지 조회합니다."
	)
	ApiResponse<Boolean> existsInQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "[테스트용] 상위 N명 입장 처리",
		description = "대기열 상위 N명을 즉시 입장 처리합니다."
	)

	@ApiErrorCode({
		"NOT_FOUND_QUEUE_ENTRY",
		"ALREADY_ENTERED",
		"ALREADY_EXPIRED",
		"ALREADY_COMPLETED",
		"NOT_WAITING_STATUS"
	})
	ApiResponse<ProcessEntriesResponse> processTopEntries(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,

		@RequestBody(required = false) @Valid ProcessEntriesRequest request
	);

	@Operation(
		summary = "[테스트용] 본인 제외 상위 사용자 모두 입장 처리",
		description = "내 앞 순번의 모든 사람을 즉시 입장 처리합니다."
	)

	@ApiErrorCode({
		"NOT_FOUND_QUEUE_ENTRY",
		"ALREADY_ENTERED",
		"ALREADY_EXPIRED",
		"ALREADY_COMPLETED",
		"NOT_WAITING_STATUS"
	})
	ApiResponse<ProcessEntriesResponse> processUntilMe(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "[테스트용] 나를 포함한 앞 사용자 모두 입장 처리",
		description = "나를 포함하여 내 앞에 있는 모든 사용자들을 입장 처리합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_QUEUE_ENTRY",
		"NOT_WAITING_STATUS",
		"NOT_INVALID_COUNT"
	})
	ApiResponse<ProcessEntriesResponse> processIncludingMe(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
