package com.back.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 인증 요청 DTO")
public record VerifyPasswordRequest(
	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(min = 8, max = 30, message = "비밀번호는 8~30 글자여야 합니다.")
	String password
) {
}
