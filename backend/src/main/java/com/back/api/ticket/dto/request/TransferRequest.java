package com.back.api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "티켓 양도 요청 DTO")
public record TransferRequest(
	@NotBlank(message = "양도 대상 닉네임을 입력해주세요.")
	@Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
	@Schema(
		description = "양도 대상 사용자 닉네임",
		example = "friend123"
	)
	String targetNickname
) {
}
