package com.back.api.payment.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentConfirmResponse;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.api.payment.payment.service.PaymentService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment", description = "결제 API")
public class PaymentController {

	private final PaymentService paymentService;
	private final HttpRequestContext httpRequestContext;

	@PostMapping("/confirm")
	@Operation(summary = "결제 승인", description = "PG사를 통한 결제를 승인하고 티켓을 발급합니다")
	public ApiResponse<PaymentConfirmResponse> confirmPayment(
		@Valid @RequestBody PaymentConfirmRequest request
	) {
		Long userId = httpRequestContext.getUser().getId();

		PaymentConfirmResponse response = paymentService.confirmPayment(
			request.orderId(),
			request.paymentKey(),
			request.amount(),
			userId
		);

		return ApiResponse.ok(
			"결제가 완료되었습니다.",
			response
		);
	}

	@GetMapping("/{orderId}/receipt")
	@Operation(
		summary = "결제 영수증 조회",
		description = "결제 완료 후 영수증 정보를 조회합니다. 주문, 티켓, 이벤트, 좌석 정보를 모두 포함합니다."
	)
	public ApiResponse<PaymentReceiptResponse> getPaymentReceipt(
		@PathVariable Long orderId
	) {
		Long userId = httpRequestContext.getUser().getId();

		PaymentReceiptResponse response = paymentService.getPaymentReceipt(orderId, userId);

		return ApiResponse.ok(
			"결제 영수증 조회 성공",
			response
		);
	}
}
