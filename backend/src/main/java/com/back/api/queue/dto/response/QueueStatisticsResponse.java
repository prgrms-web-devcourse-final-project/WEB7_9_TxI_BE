package com.back.api.queue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 통계 응답 DTO - 관리자 전용")
public record QueueStatisticsResponse(

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "전체 대기열 인원 (사전등록자 수)", example = "1000")
	Long totalCount,

	@Schema(description = "현재 대기 중인 인원 (WAITING)", example = "800")
	Long waitingCount,

	@Schema(description = "입장 완료 인원 (ENTERED, 15분 타이머 진행 중)", example = "150")
	Long enteredCount,

	@Schema(description = "만료된 인원 (EXPIRED, 시간 초과 + 마감)", example = "50")
	Long expiredCount,

	@Schema(description = "진행률 (%). (입장 완료 + 만료) / 전체 * 100", example = "20")
	Integer progress
) {
	public static QueueStatisticsResponse from(
		Long eventId,
		Long totalCount,
		Long waitingCount,
		Long enteredCount,
		Long expiredCount
	) {
		// 진행률
		int progress = totalCount > 0
			? (int) ((enteredCount + expiredCount) * 100 / totalCount)
			: 0;

		return new QueueStatisticsResponse(
			eventId,
			totalCount,
			waitingCount,
			enteredCount,
			expiredCount,
			progress
		);
	}
}

