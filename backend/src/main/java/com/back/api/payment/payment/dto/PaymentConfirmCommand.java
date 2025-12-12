package com.back.api.payment.payment.dto;

public record PaymentConfirmCommand(
	Long orderId,
	String orderKey,
	Long amount
) {
}
