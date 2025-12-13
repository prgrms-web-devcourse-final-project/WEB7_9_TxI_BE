package com.back.api.payment.payment.dto.response;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

public record PaymentConfirmResponse(
	Long orderId,
	String orderKey,
	String paymentKey,
	OrderStatus orderStatus,
	Long amount,
	Long ticketId,
	TicketStatus ticketStatus
) {
	public static PaymentConfirmResponse from(Order order, Ticket ticket) {
		return new PaymentConfirmResponse(
			order.getId(),
			order.getOrderKey(),
			order.getPaymentKey(),
			order.getStatus(),
			order.getAmount(),
			ticket.getId(),
			ticket.getTicketStatus()
		);
	}
}
