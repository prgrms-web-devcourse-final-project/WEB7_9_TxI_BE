package com.back.api.seat.dto;

import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;

public record SeatUpdateRequest(
	String seatCode,
	SeatGrade grade,
	int price,
	SeatStatus seatStatus
) {
}