package com.back.api.payment.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record V2_PaymentConfirmRequest(
	@NotNull String orderId,
	@NotBlank String paymentKey,
	@NotNull Long amount
) {
}
