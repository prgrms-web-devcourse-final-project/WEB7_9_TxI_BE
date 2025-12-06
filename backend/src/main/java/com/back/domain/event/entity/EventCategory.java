package com.back.domain.event.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "이벤트 카테고리")
@Getter
@RequiredArgsConstructor
public enum EventCategory {

	@Schema(description = "콘서트")
	CONCERT("콘서트"),

	@Schema(description = "팝업 스토어")
	POPUP("팝업"),

	@Schema(description = "한정판 드롭")
	DROP("드롭");

	private final String description;
}
