package com.back.api.preregister.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "사전등록 생성 요청 (V1 본인 인증)")
public record PreRegisterCreateRequest(

	@Schema(description = "이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	@Size(max = 50, message = "이름은 50자 이하여야 합니다.")
	String nickname,

	@Schema(description = "비밀번호 (6자 이상, 인증용)", example = "password123")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
	String password,

	@Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-01")
	@NotNull(message = "생년월일은 필수입니다.")
	@Past(message = "생년월일은 과거 날짜여야 합니다.")
	LocalDate birthDate,

	@Schema(description = "이용약관 동의 (필수)", example = "true")
	@NotNull(message = "이용약관 동의는 필수입니다.")
	Boolean agreeTerms,

	@Schema(description = "개인정보 수집 및 이용 동의 (필수)", example = "true")
	@NotNull(message = "개인정보 수집 및 이용 동의는 필수입니다.")
	Boolean agreePrivacy
) {
}
