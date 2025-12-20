package com.back.api.payment.payment.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
	@NotNull String orderId,
	@NotBlank String paymentKey,
	@NotNull Long amount
) {

}
