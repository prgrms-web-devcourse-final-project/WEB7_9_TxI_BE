package com.back.api.queue.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랜덤 큐 생성 완료 응답 DTO")
public record ShuffleQueueResponse(
	@Schema(description = "이벤트 ID", example = "2")
	Long eventId,

	@Schema(description = "랜덤 총 인원(사전 등록자 수)", example = "100")
	Integer totalCount,

	@Schema(
		description = "랜덤 큐 생성 완료 시간",
		example = "2025-12-06 12:00:00",
		type = "string",
		format = "date-time"
	)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	LocalDateTime shuffledAt
) {
	public static ShuffleQueueResponse from(
		Long eventId,
		Integer totalCount
	) {
		return new ShuffleQueueResponse(
			eventId,
			totalCount,
			LocalDateTime.now()
		);
	}
}
