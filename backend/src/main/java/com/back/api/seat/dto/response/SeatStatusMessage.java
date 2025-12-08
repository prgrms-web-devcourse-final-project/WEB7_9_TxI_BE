package com.back.api.seat.dto.response;

import lombok.Builder;

@Builder
public record SeatStatusMessage(
	Long eventId,
	Long seatId,
	String status,
	int price,
	String grade
) {

}
