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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeatController implements SeatApi {

	private final SeatService seatService;
	private final QueueEntryReadService queueEntryReadService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@GetMapping("/events/{eventId}/seats")
	public ApiResponse<List<SeatResponse>> getSeatsByEvent(
		@PathVariable Long eventId
	) {
		Long userId = httpRequestContext.getUser().getId();

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
