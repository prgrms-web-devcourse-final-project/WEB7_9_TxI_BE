package com.back.api.payment.order.dto.response;

import com.back.domain.payment.order.entity.V2_Order;

public record V2_OrderResponseDto(
	String orderId,
	Long amount,
	String orderName
) {
	public static V2_OrderResponseDto from(V2_Order order) {
		return new V2_OrderResponseDto(
			order.getOrderId(),
		order.getAmount(),
		order.getTicket().getEvent().getTitle()
		);
	}
}
