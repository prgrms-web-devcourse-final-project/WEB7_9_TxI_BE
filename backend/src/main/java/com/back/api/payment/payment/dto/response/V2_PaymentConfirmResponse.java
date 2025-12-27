package com.back.api.payment.payment.dto.response;

public record V2_PaymentConfirmResponse(
	String orderId,
	boolean success
) {
	public V2_PaymentConfirmResponse(String orderId, boolean success) {
		this.orderId = orderId;
		this.success = success;
	}
}
