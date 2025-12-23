package com.back.api.payment.payment.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentConfirmResponse;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Payment API", description = "결제 API")
public interface PaymentApi {

	@Operation(
		summary = "결제 승인",
		description = "PG사를 통한 결제를 승인하고 티켓을 발급합니다"
	)
	@ApiErrorCode({
		"ORDER_NOT_FOUND",
		"PAYMENT_ALREADY_PROCESSED",
		"PAYMENT_AMOUNT_MISMATCH",
		"PAYMENT_FAILED"
	})
	ApiResponse<PaymentConfirmResponse> confirmPayment(
		@Valid @RequestBody PaymentConfirmRequest request
	);

	@Operation(
		summary = "결제 영수증 조회",
		description = "결제 완료 후 영수증 정보를 조회합니다. 주문, 티켓, 이벤트, 좌석 정보를 모두 포함합니다."
	)
	@ApiErrorCode({
		"ORDER_NOT_FOUND",
		"PAYMENT_NOT_FOUND",
		"UNAUTHORIZED_ORDER_ACCESS"
	})
	ApiResponse<PaymentReceiptResponse> getPaymentReceipt(
		@Parameter(description = "조회할 주문 ID", example = "1")
		@PathVariable Long orderId
	);
}
