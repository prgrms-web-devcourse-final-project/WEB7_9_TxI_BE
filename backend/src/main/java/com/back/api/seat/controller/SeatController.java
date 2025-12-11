package com.back.api.seat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.dto.response.SeatResponse;
import com.back.api.seat.service.SeatService;
import com.back.domain.seat.entity.Seat;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;
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
	private final QueueEntryReadService queueEntryReadService;
	private final HttpRequestContext httpRequestContext;

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/v1/events/{eventId}/seats
	 */
	@GetMapping("/events/{eventId}/seats")
	@Operation(summary = "좌석 목록 조회", description = "특정 이벤트의 모든 좌석 목록을 조회합니다. 큐에 입장한 사용자만 조회 가능합니다.")
	public ApiResponse<List<SeatResponse>> getSeatsByEvent(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUser().getId();

		// 큐 검증
		if (!queueEntryReadService.existsInWaitingQueue(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		List<Seat> seats = seatService.getSeatsByEvent(eventId, userId);

		return ApiResponse.ok(
			"좌석 목록을 조회했습니다.",
			seats.stream().map(SeatResponse::from).toList()
		);
	}
}
