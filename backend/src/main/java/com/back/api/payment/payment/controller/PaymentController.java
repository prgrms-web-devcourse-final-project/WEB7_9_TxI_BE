package com.back.api.payment.payment.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentConfirmResponse;
import com.back.api.payment.payment.service.PaymentService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;
	private final HttpRequestContext httpRequestContext;

	@PostMapping("/confirm")
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
}
