package com.back.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 로그인 요청 DTO")
public record LoginRequest(
	@NotBlank(message = "이메일을 입력해주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 100)
	String email,

	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(min = 8, max = 30, message = "비밀번호는 8~30 글자여야 합니다.")
	String password
) {
}
