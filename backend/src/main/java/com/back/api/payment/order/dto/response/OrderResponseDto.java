package com.back.api.payment.order.dto.response;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.ticket.entity.Ticket;

public record OrderResponseDto(
	Long orderId,
	Long ticketId,
	Long amount
) {
	public static OrderResponseDto toDto(Order order, Ticket ticket) {
		return new OrderResponseDto(
			order.getId(),
			ticket.getId(),
			order.getAmount()
		);
	}
}
