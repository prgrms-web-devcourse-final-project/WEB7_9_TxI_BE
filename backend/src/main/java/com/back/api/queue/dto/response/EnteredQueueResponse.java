package com.back.api.queue.dto.response;

import java.time.LocalDateTime;

import com.back.domain.queue.entity.QueueEntryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입장 완료 상태 응답 DTO")
public record EnteredQueueResponse(
	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "이벤트 ID", example = "2")
	Long eventId,

	@Schema(description = "대기열 상태", example = "ENTERED")
	QueueEntryStatus status,

	@Schema(
		description = "입장 완료 시간",
		example = "2025-12-06 12:00:00",
		type = "string",
		format = "date-time"
	)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	LocalDateTime enteredAt,

	@Schema(
		description = "만료 예정 시간",
		example = "2025-12-06 12:15:00",
		type = "string",
		format = "date-time"
	)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	LocalDateTime expiredAt
) {
	public static EnteredQueueResponse from(
		Long userId,
		Long eventId,
		LocalDateTime enteredAt,
		LocalDateTime expiredAt
	) {
		return new EnteredQueueResponse(
			userId,
			eventId,
			QueueEntryStatus.ENTERED,
			enteredAt,
			expiredAt
		);
	}
}
