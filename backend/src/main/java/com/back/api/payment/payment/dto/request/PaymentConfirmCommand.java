package com.back.api.payment.payment.dto.request;

import java.util.UUID;

public record PaymentConfirmCommand(
	String orderId,
	Long amount
) {
}
