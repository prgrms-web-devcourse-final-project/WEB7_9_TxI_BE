package com.back.api.payment.order.dto.response;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.ticket.entity.Ticket;

public record OrderResponseDto(
	Long orderId,
	String orderKey,
	Long ticketId,
	Long amount
) {
	public static OrderResponseDto from(Order order, Ticket ticket) {
		return new OrderResponseDto(
			order.getId(),
			order.getOrderKey(),
			ticket.getId(),
			order.getAmount()
		);
	}
}
