package com.back.api.ticket.dto.response;

import java.time.LocalDateTime;

import com.back.domain.ticket.entity.Ticket;

import io.swagger.v3.oas.annotations.media.Schema;

public record TicketResponse(
	@Schema(description = "티켓 ID", example = "1")
	Long ticketId,

	@Schema(description = "이벤트 ID", example = "1")
	Long eventId,

	@Schema(description = "이벤트 제목", example = "2024 콘서트")
	String eventTitle,

	@Schema(description = "좌석 코드", example = "A1")
	String seatCode,

	@Schema(description = "좌석 등급", example = "VIP")
	String seatGrade,

	@Schema(description = "좌석 가격", example = "100000")
	int seatPrice,

	@Schema(description = "좌석 상태", example = "SOLD / RESERVED / AVAILABLE")
	String seatStatus,

	@Schema(description = "티켓 상태", example = "DRAFT / PAID / ISSUED / USED / FAILED")
	String ticketStatus,

	@Schema(description = "발급 시간", example = "2024-01-01T12:00:00")
	LocalDateTime issuedAt,

	@Schema(description = "사용 시간", example = "2024-01-01T18:00:00")
	LocalDateTime usedAt
) {
	public static TicketResponse from(Ticket ticket) {
		return new TicketResponse(
			ticket.getId(),
			ticket.getEvent().getId(),
			ticket.getEvent().getTitle(),
			ticket.getSeat().getSeatCode(),
			ticket.getSeat().getGrade().getDisplayName(),
			ticket.getSeat().getPrice(),
			ticket.getSeat().getSeatStatus().name(),
			ticket.getTicketStatus().name(),
			ticket.getIssuedAt(),
			ticket.getUsedAt()
		);
	}
}
