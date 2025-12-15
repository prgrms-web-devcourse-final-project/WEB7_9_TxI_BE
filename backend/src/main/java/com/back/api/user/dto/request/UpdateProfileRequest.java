package com.back.api.user.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 프로필 수정 요청 DTO")
public record UpdateProfileRequest(
	@Size(min = 2, max = 30, message = "이름은 2~30 글자여야 합니다.")
	String fullName,

	@Size(min = 3, max = 10, message = "닉네임은 3~10 글자여야 합니다.")
	String nickname,

	@Schema(
		description = "생년월일 중 연도",
		example = "2002"
	)
	@Pattern(regexp = "\\d{4}", message = "년도는 4자리 숫자여야 합니다.")
	String year,

	@Schema(
		description = "생년월일 중 월",
		example = "2"
	)
	@Pattern(regexp = "\\d{1,2}", message = "월은 숫자여야합니다.")
	String month,

	@Schema(
		description = "생년월일 중 일",
		example = "15"
	)
	@Pattern(regexp = "\\d{1,2}", message = "일은 숫자여야합니다.")
	String day
) {
	public LocalDate toBirthDate() {
		if (year == null || month == null || day == null) {
			return null;
		}

		return LocalDate.of(
			Integer.parseInt(year),
			Integer.parseInt(month),
			Integer.parseInt(day)
		);
	}
}
