package com.back.api.seat.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BulkCreateSeatsRequest(
	@Schema(description = "생성할 좌석 리스트")
	@NotEmpty
	@Valid
	List<SeatCreateRequest> seats
) {
}
