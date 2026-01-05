package com.back.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 토큰 요청 DTO")
public record OAuthExchangeRequest(
	@Schema(description = "인코딩된 토큰 정보")
	@NotBlank(message = "code 는 필수입니다.")
	String code
) {
}
