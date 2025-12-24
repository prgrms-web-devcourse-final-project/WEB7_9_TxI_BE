package com.back.api.queue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 맨 뒤로 이동 응답 DTO")
public record MoveToBackResponse(
	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "기존 대기열 순번", example = "3")
	int previousRank,

	@Schema(description = "밀린 대기열 순번", example = "5")
	int newRank,

	@Schema(description = "전체 대기 인원", example = "10")
	int totalWaitingUsers
) {
	public static MoveToBackResponse from (
		Long userId,
		int previousRank,
		int newRank,
		int totalWaitingUsers
	) {
		return new MoveToBackResponse(
			userId,
			previousRank,
			newRank,
			totalWaitingUsers
		);
	}
}
