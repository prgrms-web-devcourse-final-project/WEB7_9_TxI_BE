package com.back.api.payment.payment.dto.request;

public record V2_PaymentConfirmCommand(
	String orderId,
	Long amount
) {
}
