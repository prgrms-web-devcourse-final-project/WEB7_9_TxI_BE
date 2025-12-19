package com.back.api.ticket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;

import com.back.api.ticket.dto.response.TicketResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket API", description = "티켓 결제 및 조회 API")
public interface TicketApi {

	@Operation(
		summary = "내 티켓 목록 조회",
		description = "현재 로그인한 사용자의 모든 티켓 목록을 조회합니다."
	)
	ApiResponse<List<TicketResponse>> getMyTickets();

	@Operation(
		summary = "내 티켓 상세 조회",
		description = "현재 로그인한 사용자의 특정 티켓 상세 정보를 조회합니다. 티켓 ID로 조회하며, 본인의 티켓만 조회 가능합니다."
	)
	@ApiErrorCode({
		"TICKET_NOT_FOUND",
		"UNAUTHORIZED_TICKET_ACCESS"
	})
	ApiResponse<TicketResponse> getMyTicketDetails(
		@Parameter(description = "조회할 티켓 ID", example = "1")
		@PathVariable Long ticketId
	);
}
