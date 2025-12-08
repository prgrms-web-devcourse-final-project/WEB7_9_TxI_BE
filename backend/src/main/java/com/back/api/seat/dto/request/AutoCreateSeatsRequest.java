package com.back.api.seat.dto.request;

import com.back.domain.seat.entity.SeatGrade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AutoCreateSeatsRequest(
	@Schema(description = "좌석 행 개수 (A~Z, 최대 26)", example = "10")
	@NotNull
	@Min(1)
	@Max(26)
	Integer rows,

	@Schema(description = "좌석 열 개수 (1~N)", example = "20")
	@NotNull
	@Min(1)
	@Max(100)
	Integer cols,

	@Schema(description = "기본 좌석 등급", example = "R")
	@NotNull
	SeatGrade defaultGrade,

	@Schema(description = "기본 좌석 가격", example = "100000")
	@NotNull
	@Min(0)
	Integer defaultPrice
) {
}
