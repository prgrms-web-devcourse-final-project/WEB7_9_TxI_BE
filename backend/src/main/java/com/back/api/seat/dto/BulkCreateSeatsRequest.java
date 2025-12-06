package com.back.api.seat.dto;

import java.util.List;

public record BulkCreateSeatsRequest(
	List<SeatCreateRequest> seats
) {
}
