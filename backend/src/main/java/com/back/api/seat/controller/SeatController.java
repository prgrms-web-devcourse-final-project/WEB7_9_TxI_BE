package com.back.api.seat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.seat.dto.request.SelectSeatRequest;
import com.back.api.seat.dto.response.SeatResponse;
import com.back.api.seat.service.SeatService;
import com.back.domain.seat.entity.Seat;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Seat API", description = "사용자용 좌석 API")
public class SeatController {

	private final SeatService seatService;

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/v1/events/{eventId}/seats
	 */
	@GetMapping("/events/{eventId}/seats")
	@Operation(summary = "좌석 목록 조회", description = "특정 이벤트의 모든 좌석 목록을 조회합니다. 큐에 입장한 사용자만 조회 가능합니다.")
	public ApiResponse<List<SeatResponse>> getSeatsByEvent(
		@PathVariable Long eventId
	) {
		Long mockUserId = 1L; // TODO: 실제 인증된 사용자 ID로 교체 필요 (Security Context에서 가져오기)

		List<Seat> seats = seatService.getSeatsByEvent(eventId, mockUserId);

		return ApiResponse.ok(
			"좌석 목록을 조회했습니다.",
			seats.stream().map(SeatResponse::from).toList()
		);
	}

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/v1/events/{eventId}/seats/{seatId}/select
	 */
	@PostMapping("/events/{eventId}/seats/{seatId}/select")
	@Operation(summary = "좌석 선택", description = "특정 좌석을 RESERVED 상태로 변경합니다. 결제 단계가 종료되면 SOLD 상태로 변경됩니다.")
	public ApiResponse<SeatResponse> selectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId,
		@RequestBody SelectSeatRequest request
	) {
		Long mockUserId = 1L; // TODO: 실제 인증된 사용자 ID로 교체 필요 (Security Context에서 가져오기)
		Seat seat = seatService.selectSeat(eventId, seatId, mockUserId);

		return ApiResponse.ok(
			"좌석을 선택했습니다.",
			SeatResponse.from(seat)
		);
	}
}
