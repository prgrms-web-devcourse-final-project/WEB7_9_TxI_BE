package com.back.api.selection.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.selection.dto.response.SeatSelectionResponse;
import com.back.api.selection.service.SeatSelectionService;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats/{seatId}")
@RequiredArgsConstructor
public class SeatSelectionController implements SeatSelectionApi {

	private final SeatSelectionService seatSelectionService;
	private final HttpRequestContext httpRequestContext;

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/v1/events/{eventId}/seats/{seatId}/select
	 */
	@Override
	@PostMapping("/select")
	public ApiResponse<SeatSelectionResponse> selectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId
	) {
		Long userId = httpRequestContext.getUser().getId();

		Ticket draftTicket = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

		return ApiResponse.ok(
			"좌석을 선택했습니다.",
			SeatSelectionResponse.from(draftTicket)
		);
	}
}
