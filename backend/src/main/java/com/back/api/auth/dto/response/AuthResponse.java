package com.back.api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 회원가입/로그인 응답 데이터")
public record AuthResponse(

	@Schema(description = "사용자 토큰 정보")
	TokenResponse tokens,

	@Schema(description = "사용자 정보")
	UserResponse user
) {
}
