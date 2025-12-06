package com.back.api.queue.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랜덤 큐 생성 요청 DTO")
public record ShuffleQueueRequest(

	@Schema(
		description = "사전 등록한 사용자 ID 리스트",
		example = "[1, 2, 3, 4, 5]",
		minLength = 1
	)
	List<Long> preRegisteredUserIds
) {
}
