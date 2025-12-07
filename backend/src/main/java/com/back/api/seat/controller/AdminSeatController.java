package com.back.api.seat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.seat.dto.request.AutoCreateSeatsRequest;
import com.back.api.seat.dto.request.BulkCreateSeatsRequest;
import com.back.api.seat.dto.request.SeatCreateRequest;
import com.back.api.seat.dto.request.SeatUpdateRequest;
import com.back.api.seat.dto.response.SeatResponse;
import com.back.api.seat.service.AdminSeatService;
import com.back.domain.seat.entity.Seat;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSeatController {

	private final AdminSeatService adminSeatService;

	/**
	 * 좌석 대량 커스텀 생성
	 * POST /api/v1/admin/events/{eventId}/seats/bulk
	 */
	@PostMapping("/events/{eventId}/seats/bulk")
	public ApiResponse<List<SeatResponse>> bulkCreateSeats(
		@PathVariable Long eventId,
		@Valid @RequestBody BulkCreateSeatsRequest request
	) {
		List<Seat> seats = adminSeatService.bulkCreateSeats(eventId, request.seats());
		List<SeatResponse> responses = seats.stream()
			.map(SeatResponse::from)
			.toList();

		return ApiResponse.created(
			"좌석을 대량 생산했습니다",
			responses
		);
	}

	/**
	 * 좌석 자동 생성 (행-열 기반)
	 * POST /api/v1/admin/events/{eventId}/seats/auto
	 */
	@PostMapping("/events/{eventId}/seats/auto")
	public ApiResponse<List<SeatResponse>> autoCreateSeats(
		@PathVariable Long eventId,
		@Valid @RequestBody AutoCreateSeatsRequest request
	) {
		List<Seat> seats = adminSeatService.autoCreateSeats(eventId, request);
		List<SeatResponse> responses = seats.stream()
			.map(SeatResponse::from)
			.toList();

		return ApiResponse.created(
			String.format("좌석을 자동 생성했습니다. (총 %d개: %s행 x %s열)",
				seats.size(), request.rows(), request.cols()),
			responses
		);
	}

	/**
	 * 단일 좌석 생성
	 * POST /api/v1/admin/events/{eventId}/seats/single
	 */
	@PostMapping("/events/{eventId}/seats/single")
	public ApiResponse<SeatResponse> createSingleSeat(
		@PathVariable Long eventId,
		@RequestBody SeatCreateRequest request
	) {
		Seat seat = adminSeatService.createSingleSeat(eventId, request);

		return ApiResponse.created("좌석을 생성했습니다.", SeatResponse.from(seat));
	}

	/**
	 * 좌석 수정
	 * PUT /api/v1/admin/seats/{seatId}
	 */
	@PutMapping("/seats/{seatId}")
	public ApiResponse<SeatResponse> updateSeat(
		@PathVariable Long seatId,
		@RequestBody SeatUpdateRequest request
	) {
		Seat seat = adminSeatService.updateSeat(seatId, request);

		return ApiResponse.ok("좌석을 수정했습니다.", SeatResponse.from(seat));
	}

	/**
	 * 단일 좌석 삭제
	 * DELETE /api/v1/admin/seats/{seatId}
	 */
	@DeleteMapping("/seats/{seatId}")
	public ApiResponse<Void> deleteSeat(
		@PathVariable Long seatId
	) {
		adminSeatService.deleteSeat(seatId);

		return ApiResponse.noContent("좌석을 삭제했습니다.");
	}

	/**
	 * 이벤트의 모든 좌석 삭제
	 * DELETE /api/v1/admin/events/{eventId}/seats
	 */
	@DeleteMapping("/events/{eventId}/seats")
	public ApiResponse<Void> deleteAllEventSeats(
		@PathVariable Long eventId
	) {
		adminSeatService.deleteAllEventSeats(eventId);

		return ApiResponse.noContent("이벤트의 모든 좌석을 삭제했습니다.");
	}
}
