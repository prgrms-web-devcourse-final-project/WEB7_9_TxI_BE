package com.back.api.seat.dto;

import com.back.domain.seat.entity.Seat;

public record SeatResponse(
	Long id,
	Long eventId,
	String seatCode,
	String grade,
	int price,
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