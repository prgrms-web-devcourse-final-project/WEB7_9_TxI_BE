package com.back.api.ticket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.api.ticket.service.TicketService;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

	private final TicketService ticketService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@GetMapping("/my")
	public ApiResponse<List<TicketResponse>> getMyTickets() {
		Long userId = httpRequestContext.getUserId();

		List<TicketResponse> responses = ticketService.getMyTickets(userId);

		return ApiResponse.ok("사용자의 티켓 목록 조회 성공", responses);
	}

	@Override
	@GetMapping("/my/{ticketId}/details")
	public ApiResponse<TicketResponse> getMyTicketDetails(
		@PathVariable Long ticketId
	) {
		Long userId = httpRequestContext.getUserId();

		Ticket ticket = ticketService.getTicketDetail(ticketId, userId);

		return ApiResponse.ok(
			"사용자의 티켓 상세 조회 성공",
			TicketResponse.from(ticket)
		);
	}
}
