package com.back.domain.queue.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "큐 엔트리 상태")
public enum QueueEntryStatus {

	@Schema(description = "대기 중")
	WAITING,

	@Schema(description = "입장 완료")
	ENTERED,

	@Schema(description = "결제 시간 만료")
	EXPIRED
}
