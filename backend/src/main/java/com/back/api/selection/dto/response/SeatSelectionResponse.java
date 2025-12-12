package com.back.api.selection.dto.response;

import com.back.domain.ticket.entity.Ticket;

public record SeatSelectionResponse(
	Long ticketId,
	Long eventId,
	Long seatId,
	String seatCode,
	String seatGrade,
	int seatPrice,
	String seatStatus,
	String ticketStatus
) {
	public static SeatSelectionResponse from(Ticket ticket) {
		return new SeatSelectionResponse(
			ticket.getId(),
			ticket.getEvent().getId(),
			ticket.getSeat().getId(),
			ticket.getSeat().getSeatCode(),
			ticket.getSeat().getGrade().name(),
			ticket.getSeat().getPrice(),
			ticket.getSeat().getSeatStatus().name(),
			ticket.getTicketStatus().name()
		);
	}
}
