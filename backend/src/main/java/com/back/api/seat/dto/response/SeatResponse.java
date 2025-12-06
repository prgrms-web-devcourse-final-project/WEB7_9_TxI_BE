package com.back.api.seat.dto.response;

import com.back.domain.seat.entity.Seat;

import io.swagger.v3.oas.annotations.media.Schema;

public record SeatResponse(
	@Schema(description = "좌석 id", example = "1")
	Long id,
	@Schema(description = "이벤트 id", example = "1")
	Long eventId,
	@Schema(description = "좌석 코드", example = "A1")
	String seatCode,
	@Schema(description = "좌석 등급", example = "VIP")
	String grade,
	@Schema(description = "좌석 가격", example = "100000")
	int price,
	@Schema(description = "좌석 상태", example = "AVAILABLE / SOLD / RESERVED")
	String seatStatus
) {
	public static SeatResponse from(Seat seat) {
		return new SeatResponse(
			seat.getId(),
			seat.getEvent().getId(),
			seat.getSeatCode(),
			seat.getGrade().getDisplayName(),
			seat.getPrice(),
			seat.getSeatStatus().name()
		);
	}
}
