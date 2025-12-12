package com.back.api.payment.payment.dto;

public record PaymentConfirmResult(
	String paymentKey,
	Long approvedAmount,
	boolean success
) {
}