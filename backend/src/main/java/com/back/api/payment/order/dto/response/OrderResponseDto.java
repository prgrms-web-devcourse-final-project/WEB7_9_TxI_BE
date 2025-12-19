package com.back.api.payment.order.dto.response;

import java.util.UUID;

import com.back.domain.payment.order.entity.Order;

public record OrderResponseDto(
	UUID orderId,
	Long amount,
	String orderName
) {
	public static OrderResponseDto from(Order order) {
		return new OrderResponseDto(
			order.getOrderId(),
			order.getAmount(),
			order.getTicket().getEvent().getTitle()

		);
	}
}
