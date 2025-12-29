package com.back.api.payment.payment.dto.response;

public record V2_PaymentConfirmResult(
	String paymentKey,
	Long approvedAmount,
	boolean success
) {
}
