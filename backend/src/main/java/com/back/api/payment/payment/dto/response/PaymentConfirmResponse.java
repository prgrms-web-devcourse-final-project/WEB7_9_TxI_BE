package com.back.api.payment.payment.dto.response;

import java.util.UUID;

import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;

public record PaymentConfirmResponse(
	String orderId,
	boolean success
) {
	public PaymentConfirmResponse(String orderId, boolean success) {
		this.orderId = orderId;
		this.success = success;
	}
}
