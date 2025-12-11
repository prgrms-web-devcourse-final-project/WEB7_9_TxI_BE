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
public class AdminSeatController implements AdminSeatApi {

	private final AdminSeatService adminSeatService;

	@Override
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

	@Override
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

	@Override
	@PostMapping("/events/{eventId}/seats/single")
	public ApiResponse<SeatResponse> createSingleSeat(
		@PathVariable Long eventId,
		@Valid @RequestBody SeatCreateRequest request
	) {
		Seat seat = adminSeatService.createSingleSeat(eventId, request);

		return ApiResponse.created("좌석을 생성했습니다.", SeatResponse.from(seat));
	}

	@Override
	@PutMapping("/events/{eventId}/seats/{seatId}")
	public ApiResponse<SeatResponse> updateSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId,
		@Valid @RequestBody SeatUpdateRequest request
	) {
		Seat seat = adminSeatService.updateSeat(seatId, request);

		return ApiResponse.ok("좌석을 수정했습니다.", SeatResponse.from(seat));
	}

	@Override
	@DeleteMapping("/events/{eventId}/seats/{seatId}")
	public ApiResponse<Void> deleteSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId
	) {
		adminSeatService.deleteSeat(seatId);

		return ApiResponse.noContent("좌석을 삭제했습니다.");
	}

	@Override
	@DeleteMapping("/events/{eventId}/seats")
	public ApiResponse<Void> deleteAllEventSeats(
		@PathVariable Long eventId
	) {
		adminSeatService.deleteAllEventSeats(eventId);

		return ApiResponse.noContent("이벤트의 모든 좌석을 삭제했습니다.");
	}
}
