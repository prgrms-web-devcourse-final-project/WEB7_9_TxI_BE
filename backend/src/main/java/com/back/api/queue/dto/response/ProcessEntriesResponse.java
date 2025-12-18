package com.back.api.queue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "[테스트용] 대기열 입장 처리 응답 DTO")
public record ProcessEntriesResponse(

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "입장 처리된 인원 수", example = "5")
	int processedCount,

	@Schema(description = "남은 대기 인원 수", example = "50")
	long remainingWaitingCount
) {
	public static ProcessEntriesResponse from(
		Long eventId,
		int processedCount,
		long remainingWaitingCount
	) {
		return new ProcessEntriesResponse(
			eventId,
			processedCount,
			remainingWaitingCount
		);
	}
}
