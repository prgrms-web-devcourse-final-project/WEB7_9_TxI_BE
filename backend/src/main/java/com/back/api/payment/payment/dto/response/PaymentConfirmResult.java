package com.back.api.payment.payment.dto.response;

public record PaymentConfirmResult(
	String paymentKey,
	Long approvedAmount,
	boolean success
) {
}