package com.back.api.queue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
	description = "대기열 상태 응답",
	oneOf = {
		WaitingQueueResponse.class,
		EnteredQueueResponse.class,
		ExpiredQueueResponse.class,
		CompletedQueueResponse.class
	}
)
public interface QueueEntryStatusResponse {
	Long userId();

	Long eventId();
}
