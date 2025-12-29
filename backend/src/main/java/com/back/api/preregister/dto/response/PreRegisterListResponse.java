package com.back.api.preregister.dto.response;

import java.time.LocalDateTime;

import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 사전 등록 항목 응답 DTO")
public record PreRegisterListResponse(

	@Schema(description = "사전 등록 ID", example = "1")
	Long id,

	@Schema(description = "사용자 이메일", example = "test@test.com")
	String userEmail,

	@Schema(description = "사전 등록 시간", example = "2025-12-29T09:51:16")
	LocalDateTime createdAt,

	@Schema(description = "사전 등록 상태", example = "REGISTERED")
	PreRegisterStatus preRegisterStatus

) {

	public static PreRegisterListResponse from(PreRegister preRegister) {
		return new PreRegisterListResponse(
			preRegister.getId(),
			preRegister.getUser().getEmail(),
			preRegister.getCreateAt(),
			preRegister.getPreRegisterStatus()
		);
	}
}