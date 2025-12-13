package com.back.api.seat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.seat.dto.response.SeatResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Seat API", description = "사용자용 좌석 API")
public interface SeatApi {

	@Operation(
		summary = "좌석 목록 조회",
		description = "특정 이벤트의 모든 좌석 목록을 조회합니다. 큐에 입장한 사용자만 조회 가능합니다."
	)
	@ApiErrorCode("NOT_IN_QUEUE")
	ApiResponse<List<SeatResponse>> getSeatsByEvent(
		@Parameter(description = "이벤트 ID", example = "1")
		@PathVariable Long eventId
	);
}
