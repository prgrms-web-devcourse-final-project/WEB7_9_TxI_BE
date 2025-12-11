package com.back.api.seat.dto.response;

import com.back.domain.seat.entity.Seat;

import lombok.Builder;

@Builder
public record SeatStatusMessage(
	Long eventId,
	Long seatId,
	String status,
	int price,
	String grade
) {
	public static SeatStatusMessage from(Seat seat) {
		return new SeatStatusMessage(
			seat.getEvent().getId(),
			seat.getId(),
			seat.getSeatStatus().name(),
			seat.getPrice(),
			seat.getGrade().name()
		);
	}
}
