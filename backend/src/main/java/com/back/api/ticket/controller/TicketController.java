package com.back.api.ticket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.api.ticket.service.TicketService;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

	private final TicketService ticketService;

	@Override
	@PostMapping("/{ticketId}/payment/success")
	public ApiResponse<TicketResponse> confirmPayment(
		@PathVariable Long ticketId
	) {
		Long userId = 1L;

		Ticket ticket = ticketService.confirmPayment(ticketId, userId);

		return ApiResponse.ok(
			"결제가 완료되었습니다.",
			TicketResponse.from(ticket)
		);
	}

	@Override
	@PostMapping("/{ticketId}/payment/fail")
	public ApiResponse<Void> failPayment(
		@PathVariable Long ticketId
	) {
		ticketService.failPayment(ticketId);
		return ApiResponse.noContent("결제 실패 처리 완료");
	}

	@Override
	@GetMapping("/my")
	public ApiResponse<List<TicketResponse>> getMyTickets() {
		Long userId = 1L;

		List<Ticket> tickets = ticketService.getMyTickets(userId);

		return ApiResponse.ok(
			"사용자의 티켓 목록 조회 성공",
			tickets.stream().map(TicketResponse::from).toList()
		);
	}
}