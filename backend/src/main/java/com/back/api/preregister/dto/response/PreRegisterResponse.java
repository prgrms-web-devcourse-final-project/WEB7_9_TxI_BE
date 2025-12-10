package com.back.api.preregister.dto.response;

import java.time.LocalDateTime;

import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사전등록 응답")
public record PreRegisterResponse(

	@Schema(description = "사전등록 ID", example = "1")
	Long id,

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "사용자 ID", example = "1")
	Long userId,

	@Schema(description = "사전등록 상태", example = "REGISTERED")
	PreRegisterStatus status,

	@Schema(description = "등록일시", example = "2025-12-10T10:00:00")
	LocalDateTime createdAt
) {

	public static PreRegisterResponse from(PreRegister preRegister) {
		return new PreRegisterResponse(
			preRegister.getId(),
			preRegister.getEventId(),
			preRegister.getUserId(),
			preRegister.getPreRegisterStatus(),
			preRegister.getCreateAt()
		);
	}
}
