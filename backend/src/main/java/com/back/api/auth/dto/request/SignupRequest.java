package com.back.api.auth.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 회원가입 요청 DTO")
public record SignupRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 100)
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 30, message = "비밀번호는 8~30 글자여야 합니다.")
	String password,

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(min = 3, max = 10, message = "닉네임은 3~10 글자여야 합니다.")
	String nickname,

	@Schema(
		description = "생년월일 중 연도",
		example = "2002"
	)
	@NotBlank(message = "생년월일은 필수입니다.")
	@Pattern(regexp = "\\d{4}", message = "년도는 4자리 숫자여야 합니다.")
	String year,

	@Schema(
		description = "생년월일 중 월",
		example = "2"
	)
	@NotBlank(message = "생년월일은 필수입니다.")
	@Pattern(regexp = "\\d{1,2}", message = "월은 숫자여야합니다.")
	String month,

	@Schema(
		description = "생년월일 중 일",
		example = "15"
	)
	@NotBlank(message = "생년월일은 필수입니다.")
	@Pattern(regexp = "\\d{1,2}", message = "일은 숫자여야합니다.")
	String day
) {
	public LocalDate toBirthDate() {
		return LocalDate.of(
			Integer.parseInt(year),
			Integer.parseInt(month),
			Integer.parseInt(day)
		);
	}
}
