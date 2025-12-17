package com.back.api.selection.controller;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.selection.dto.response.SeatSelectionResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Seat Selection API", description = "좌석 선택 및 Draft Ticket 생성 API")
public interface SeatSelectionApi {

	@Operation(summary = "좌석 선택", description = "특정 좌석을 RESERVED 상태로 변경합니다. 결제 단계가 종료되면 SOLD 상태로 변경됩니다.")
	@ApiErrorCode(
		{
			"NOT_FOUND_SEAT",
			"SEAT_CONCURRENCY_FAILURE",
			"NOT_FOUND_USER",
			"NOT_FOUND_EVENT",
			"SEAT_ALREADY_PURCHASED",
			"TICKET_ALREADY_IN_PROGRESS",
		}
	)
	public ApiResponse<SeatSelectionResponse> selectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId
	);

	@Operation(summary = "좌석 선택 취소", description = "선택한 좌석을 취소하고 AVAILABLE 상태로 복구합니다. Draft Ticket은 유지됩니다.")
	@ApiErrorCode(
		{
			"NOT_FOUND_SEAT",
			"SEAT_NOT_SELECTED",
			"NOT_FOUND_USER",
		}
	)
	public ApiResponse<Void> deselectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId
	);
}
