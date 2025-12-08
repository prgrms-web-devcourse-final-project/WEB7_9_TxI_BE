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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeatController {

	private final SeatService seatService;

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/v1/events/{eventId}/seats
	 */
	@GetMapping("/events/{eventId}/seats")
	public ApiResponse<List<SeatResponse>> getSeatsByEvent(
		@PathVariable Long eventId
	) {
		List<Seat> seats = seatService.getSeatsByEvent(eventId);

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
	public ApiResponse<SeatResponse> selectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId,
		@RequestBody SelectSeatRequest request
	) {
		// TODO: 실제 인증된 사용자 ID로 교체 필요 (Security Context에서 가져오기)
		Seat seat = seatService.selectSeat(eventId, seatId, request.userId());

		return ApiResponse.ok(
			"좌석을 선택했습니다.",
			SeatResponse.from(seat)
		);
	}
}
