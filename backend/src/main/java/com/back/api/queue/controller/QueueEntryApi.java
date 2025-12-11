package com.back.api.queue.controller;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.queue.dto.response.QueueEntryStatusResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "QueueEntry API", description = "사용자 대기열 API")
public interface QueueEntryApi {

	@Operation(
		summary = "내 대기열 상태 조회",
		description = "인증된 사용자의 현재 대기열 상태를 조회합니다. " +
					  "JWT 토큰을 통해 사용자를 식별하므로 userId 파라미터가 필요하지 않습니다.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@ApiErrorCode({
		"UNAUTHORIZED",
		"NOT_FOUND_QUEUE_ENTRY",
		"NOT_WAITING_STATUS"
	})
	ApiResponse<QueueEntryStatusResponse> getMyQueueEntryStatus(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);

	@Operation(
		summary = "대기열 진입 여부 조회",
		description = "인증된 사용자가 특정 이벤트의 대기열에 진입했는지 조회합니다. " +
					  "JWT 토큰을 통해 사용자를 식별하므로 userId 파라미터가 필요하지 않습니다.",
		security = @SecurityRequirement(name = "Bearer Authentication")
	)
	@ApiErrorCode({
		"UNAUTHORIZED"
	})
	ApiResponse<Boolean> existsInQueue(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
