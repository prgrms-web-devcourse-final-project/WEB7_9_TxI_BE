package com.back.api.queue.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "랜덤 큐 생성 요청 DTO")
public record ShuffleQueueRequest(

	@NotNull(message = "사전 등록한 사용자 ID 리스트는 필수입니다.")
	@NotEmpty(message = "사전 등록한 사용자 ID 리스트는 최소 1명 이상이어야 합니다.")
	@Schema(
		description = "사전 등록한 사용자 ID 리스트",
		example = "[1, 2, 3, 4, 5]",
		minLength = 1
	)
	List<Long> preRegisteredUserIds
) {
}
