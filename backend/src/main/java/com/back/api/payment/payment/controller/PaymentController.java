package com.back.api.payment.payment.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.request.V2_PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.api.payment.payment.dto.response.V2_PaymentConfirmResponse;
import com.back.api.payment.payment.service.PaymentService;
import com.back.global.http.HttpRequestContext;
import com.back.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController implements PaymentApi {

	private final PaymentService paymentService;
	private final HttpRequestContext httpRequestContext;

	@Override
	@PostMapping("/v1/payments/confirm")
	public ApiResponse<PaymentReceiptResponse> confirmPayment(
		@Valid @RequestBody PaymentConfirmRequest request
	) {
		Long userId = httpRequestContext.getUser().getId();

		PaymentReceiptResponse response = paymentService.confirmPayment(
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

	/**
	 * 결제 post요청에 응답값에 영수증 정보가 모두 담기는 것으로 변경,
	 * 추후 PG연동 대비 보관처리
	 @Override
	 @GetMapping("/{orderId}/receipt") public ApiResponse<PaymentReceiptResponse> getPaymentReceipt(
	 @PathVariable Long orderId
	 ) {
	 Long userId = httpRequestContext.getUser().getId();

	 PaymentReceiptResponse response = paymentService.getPaymentReceipt(orderId, userId);

	 return ApiResponse.ok(
	 "결제 영수증 조회 성공",
	 response
	 );
	 }
	 */

	@PostMapping("/v2/payments/confirm")
	public ApiResponse<V2_PaymentConfirmResponse> V2_confirmPayment(
		@Valid @RequestBody V2_PaymentConfirmRequest request
	) {

		Long userId = httpRequestContext.getUser().getId();

		V2_PaymentConfirmResponse response = paymentService.v2_confirmPayment(
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
