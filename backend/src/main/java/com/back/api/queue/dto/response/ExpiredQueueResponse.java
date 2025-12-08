package com.back.api.queue.dto.response;

import com.back.domain.queue.entity.QueueEntryStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "만료 상태 응답 DTO")
public record ExpiredQueueResponse(
	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "이벤트 ID", example = "2")
	Long eventId,

	@Schema(description = "대기열 상태", example = "EXPIRED")
	QueueEntryStatus status
) implements QueueEntryStatusResponse {
	public static ExpiredQueueResponse from(
		Long userId,
		Long eventId
	) {
		return new ExpiredQueueResponse(
			userId,
			eventId,
			QueueEntryStatus.EXPIRED
		);
	}
}
