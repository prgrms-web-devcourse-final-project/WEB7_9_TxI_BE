package com.back.api.seat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.seat.dto.request.AutoCreateSeatsRequest;
import com.back.api.seat.dto.request.BulkCreateSeatsRequest;
import com.back.api.seat.dto.request.SeatCreateRequest;
import com.back.api.seat.dto.request.SeatUpdateRequest;
import com.back.api.seat.dto.response.SeatResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Seat Admin API", description = "관리자용 좌석관리 API")
public interface AdminSeatApi {

	@Operation(
		summary = "좌석 대량 커스텀 생성",
		description = "JSON형태로 지정한 좌석들을 한 번에 생성합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"DUPLICATE_SEAT_CODE"
	})
	ApiResponse<List<SeatResponse>> bulkCreateSeats(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Valid @RequestBody BulkCreateSeatsRequest request
	);

	@Operation(
		summary = "좌석 대량 자동 생성",
		description = "지정한 행-열 수에 따라 좌석들을 한 번에 생성합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"DUPLICATE_SEAT_CODE"
	})
	ApiResponse<List<SeatResponse>> autoCreateSeats(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Valid @RequestBody AutoCreateSeatsRequest request
	);

	@Operation(
		summary = "좌석 단일 생성",
		description = "단일 좌석을 생성합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_EVENT",
		"DUPLICATE_SEAT_CODE"
	})
	ApiResponse<SeatResponse> createSingleSeat(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Valid @RequestBody SeatCreateRequest request
	);

	@Operation(
		summary = "좌석 수정",
		description = "단일 좌석을 수정합니다."
	)
	@ApiErrorCode({
		"NOT_FOUND_SEAT",
		"DUPLICATE_SEAT_CODE"
	})
	ApiResponse<SeatResponse> updateSeat(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "좌석 ID", example = "1")
		@PathVariable Long seatId,
		@Valid @RequestBody SeatUpdateRequest request
	);

	@Operation(
		summary = "좌석 단일 삭제",
		description = "단일 좌석을 삭제합니다."
	)
	@ApiErrorCode("NOT_FOUND_SEAT")
	ApiResponse<Void> deleteSeat(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId,
		@Parameter(description = "좌석 ID", example = "1")
		@PathVariable Long seatId
	);

	@Operation(
		summary = "좌석 전량 삭제",
		description = "이벤트의 모든 좌석을 삭제합니다."
	)
	ApiResponse<Void> deleteAllEventSeats(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
