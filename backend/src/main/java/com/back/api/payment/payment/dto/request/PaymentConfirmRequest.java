package com.back.api.payment.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
	@NotNull Long orderId,
	@NotBlank String paymentKey,
	@NotNull Long amount
) {
}
