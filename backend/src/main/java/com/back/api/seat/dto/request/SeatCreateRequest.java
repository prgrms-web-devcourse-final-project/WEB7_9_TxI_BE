package com.back.api.seat.dto.request;

import com.back.domain.seat.entity.SeatGrade;

import io.swagger.v3.oas.annotations.media.Schema;

public record SeatCreateRequest(
	@Schema(description = "좌석 코드", example = "A1")
	String seatCode,
	@Schema(description = "좌석 등급", example = "VIP")
	SeatGrade grade,
	@Schema(description = "좌석 가격", example = "100000")
	int price
) {
}
