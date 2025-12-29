package com.back.api.queue.dto.response;

import java.time.LocalDateTime;

import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 대기열 항목 응답 DTO")
public record QueueEntryListResponse(
	@Schema(description = "대기열 ID", example = "1")
	Long id,

	@Schema(description = "대기 순번", example = "1")
	Integer queueRank,

	@Schema(description = "사용자 이메일", example = "user@example.com")
	String userEmail,

	@Schema(description = "대기열 생성 시간", example = "2025-12-29T09:51:16")
	LocalDateTime createdAt,

	@Schema(description = "입장 시간", example = "2025-12-29T10:25:16")
	LocalDateTime enteredAt,

	@Schema(description = "만료 시간", example = "2025-12-29T10:40:16")
	LocalDateTime expiredAt,

	@Schema(description = "대기열 상태", example = "WAITING")
	QueueEntryStatus queueEntryStatus,

	@Schema(description = "상태", example = "대기중")
	String statusText
) {
	public static QueueEntryListResponse from(QueueEntry queueEntry) {
		return new QueueEntryListResponse(
			queueEntry.getId(),
			queueEntry.getQueueRank(),
			queueEntry.getUser().getEmail(),
			queueEntry.getCreateAt(),
			queueEntry.getEnteredAt(),
			queueEntry.getExpiredAt(),
			queueEntry.getQueueEntryStatus(),
			getStatusText(queueEntry.getQueueEntryStatus())
		);
	}
	private static String getStatusText(QueueEntryStatus status) {
		return switch (status) {
			case WAITING -> "대기중";
			case ENTERED -> "입장";
			case COMPLETED -> "결제 완료";
			case EXPIRED -> "만료";
		};
	}

}