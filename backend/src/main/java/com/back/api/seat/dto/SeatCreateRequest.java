package com.back.api.seat.dto;

import com.back.domain.seat.entity.SeatGrade;

public record SeatCreateRequest(
	String seatCode,
	SeatGrade grade,
	int price
) {
}