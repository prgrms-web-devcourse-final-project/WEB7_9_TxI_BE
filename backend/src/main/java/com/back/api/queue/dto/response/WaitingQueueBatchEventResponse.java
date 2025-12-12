package com.back.api.queue.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

// 대기열 일괄 업데이트 이벤트
// broadcast 방식으로 모든 대기자에게 한번에 전송
@Schema(description = "대기열 BroadCast DTO")
public record WaitingQueueBatchEventResponse(

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "대기열 사용자별 실시간 상태 업데이트 맵")
	Map<Long, WaitingQueueResponse> updates
) {
	public static WaitingQueueBatchEventResponse from(Long eventId, Map<Long, WaitingQueueResponse> updates) {
		return new WaitingQueueBatchEventResponse(eventId, updates);
	}
}
