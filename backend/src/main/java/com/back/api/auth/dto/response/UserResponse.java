package com.back.api.auth.dto.response;

import com.back.domain.user.entity.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 인증 정보 응답 DTO")
public record UserResponse(
	@Schema(
		description = "사용자 아이디 정보",
		example = "1"
	)
	Long userId,

	@Schema(
		description = "사용자 이메일",
		example = "user@example.com",
		maxLength = 100
	)
	String email,

	@Schema(
		description = "사용자 닉네임",
		maxLength = 20
	)
	String nickname,

	@Schema(
		description = "사용자 권한 범위"
	)
	UserRole role
) {
}
