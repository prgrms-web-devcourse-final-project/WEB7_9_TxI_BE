package com.back.api.user.dto.response;

import com.back.domain.user.entity.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 상세 정보 DTO")
public record UserProfileResponse(
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
		description = "사용자 이름",
		maxLength = 30
	)
	String fullName,

	@Schema(
		description = "사용자 닉네임",
		maxLength = 20
	)
	String nickname,

	@Schema(
		description = "사용자 권한 범위"
	)
	UserRole role,

	@Schema(
		description = "사용자 생년월일",
		example = "yyyy-MM-dd"
	)
	String birthDate,

	@Schema(
		description = "사용자 서비스 가입일",
		example = "yyyy-MM-dd"
	)
	String signupDate
) {
}
