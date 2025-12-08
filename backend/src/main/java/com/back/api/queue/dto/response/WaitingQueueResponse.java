package com.back.api.queue.dto.response;

import com.back.domain.queue.entity.QueueEntryStatus;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "대기 중 상태 응답 DTO")
public record WaitingQueueResponse(

	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "이벤트 ID", example = "2")
	Long eventId,

	@Schema(description = "대기열 상태", example = "WAITING")
	QueueEntryStatus status,

	@Schema(description = "대기열 순위", example = "5", minimum = "1")
	Integer queueRank,

	@Schema(description = "앞에 대기 중인 인원 수", example = "4", minimum = "0")
	Integer waitingAhead,

	@Schema(description = "예상 대기 시간(분)", example = "15", minimum = "0")
	Integer estimatedWaitTime,

	@Schema(description = "대기열 진행률(%)", example = "30", minimum = "0", maximum = "100")
	Integer progress //진행률 백/프론트 중 고민
) implements QueueEntryStatusResponse {

	public static WaitingQueueResponse from(
		Long userId,
		Long eventId,
		int queueRank,
		int waitingAhead,
		int estimatedWaitTime,
		int progress
	) {
		return new WaitingQueueResponse(
			userId,
			eventId,
			QueueEntryStatus.WAITING,
			queueRank,
			waitingAhead,
			estimatedWaitTime,
			progress
		);
	}
}
