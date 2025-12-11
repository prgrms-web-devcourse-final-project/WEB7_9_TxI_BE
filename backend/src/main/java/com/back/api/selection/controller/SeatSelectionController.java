package com.back.api.selection.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.seat.dto.request.SelectSeatRequest;
import com.back.api.selection.service.SeatSelectionService;
import com.back.api.ticket.dto.response.TicketResponse;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats/{seatId}")
@RequiredArgsConstructor
@Tag(name = "Seat Selection API", description = "좌석 선택 및 Draft Ticket 생성 API")
public class SeatSelectionController {

	private final SeatSelectionService seatSelectionService;
	private final HttpRequestContext httpRequestContext;

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/v1/events/{eventId}/seats/{seatId}/select
	 */
	@PostMapping("/select")
	@Operation(summary = "좌석 선택", description = "특정 좌석을 RESERVED 상태로 변경합니다. 결제 단계가 종료되면 SOLD 상태로 변경됩니다.")
	public ApiResponse<TicketResponse> selectSeat(
		@PathVariable Long eventId,
		@PathVariable Long seatId,
		@RequestBody SelectSeatRequest request
	) {
		Long userId = httpRequestContext.getUser().getId();

		Ticket draftTicket = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

		return ApiResponse.ok(
			"좌석을 선택했습니다.",
			TicketResponse.from(draftTicket)
		);
	}
}
