package com.back.api.payment.payment.dto.request;

public record PaymentConfirmCommand(
	Long orderId,
	String orderKey,
	Long amount
) {
}
