package com.back.api.preregister.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "사전등록 생성 요청")
public record PreRegisterCreateRequest(

	@Schema(description = "이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	String name,

	@Schema(description = "비밀번호 (6자 이상)", example = "password123")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
	String password,

	@Schema(description = "생년월일", example = "1990-01-01")
	@NotNull(message = "생년월일은 필수입니다.")
	@Past(message = "생년월일은 과거 날짜여야 합니다.")
	LocalDate birthDate
) {
}
