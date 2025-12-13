package com.back.api.ticket.controller;

import java.util.List;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket API", description = "티켓 결제 및 조회 API")
public interface TicketApi {

	/*
	@Operation(
		summary = "결제 성공",
		description = "Draft Ticket을 ISSUED 상태로 변경합니다. 내부 로직 또는 PG webhook 호출용 엔드포인트입니다."
	)
	@ApiErrorCode({
		"TICKET_NOT_FOUND",
		"UNAUTHORIZED_TICKET_ACCESS",
		"INVALID_TICKET_STATE"
	})
	ApiResponse<TicketResponse> confirmPayment(
		@Parameter(description = "티켓 ID", example = "1")
		@PathVariable Long ticketId
	);

	@Operation(
		summary = "결제 실패",
		description = "Draft Ticket을 FAILED로 변경하고 좌석을 AVAILABLE로 복구합니다. 내부 로직 또는 PG webhook 호출용 엔드포인트입니다."
	)
	@ApiErrorCode({
		"TICKET_NOT_FOUND",
		"INVALID_TICKET_STATE"
	})
	ApiResponse<Void> failPayment(
		@Parameter(description = "티켓 ID", example = "1")
		@PathVariable Long ticketId
	);
	*/

	@Operation(
		summary = "내 티켓 목록 조회",
		description = "현재 로그인한 사용자의 모든 티켓 목록을 조회합니다."
	)
	ApiResponse<List<TicketResponse>> getMyTickets();
}
