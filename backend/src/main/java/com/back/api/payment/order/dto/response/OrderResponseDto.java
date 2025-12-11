package com.back.api.payment.order.dto.response;

import com.back.domain.payment.order.entity.Order;

public record OrderResponseDto(
	Long amount
) {
	public static OrderResponseDto toDto(Order order) {
		return new OrderResponseDto(
			order.getAmount()
		);
	}
}
