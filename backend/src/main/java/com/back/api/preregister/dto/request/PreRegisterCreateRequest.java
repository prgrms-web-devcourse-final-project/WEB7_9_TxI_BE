package com.back.api.preregister.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사전등록 생성 요청 (SMS 본인 인증)")
public record PreRegisterCreateRequest(

	@Schema(description = "이름 (회원정보의 fullName과 대조)", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 30, message = "이름은 2자 이상 30자 이하여야 합니다.")
	String fullName,

	@Schema(description = "휴대폰 번호 (하이픈 제거)", example = "01012345678")
	@NotBlank(message = "휴대폰 번호는 필수입니다.")
	@Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
	String phoneNumber,

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
