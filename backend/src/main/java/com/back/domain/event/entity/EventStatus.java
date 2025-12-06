package com.back.domain.event.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "이벤트 진행 상태")
@Getter
@RequiredArgsConstructor
public enum EventStatus {

	@Schema(description = "준비중 - 이벤트가 아직 시작되지 않음")
	READY("준비중"),

	@Schema(description = "사전등록중 - 사전등록 기간")
	PRE_OPEN("사전등록중"),

	@Schema(description = "대기열 준비 - 티켓팅 직전 대기열 구성 중")
	QUEUE_READY("대기열 준비"),

	@Schema(description = "티켓팅 진행중 - 실제 티켓 구매 가능")
	OPEN("티켓팅 진행중"),

	@Schema(description = "마감 - 이벤트 종료")
	CLOSED("마감");

	private final String description;
}
