package com.back.api.payment.payment.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;

import lombok.RequiredArgsConstructor;


// 백엔드 <-> 토스페이먼츠 api 서버
@Service
@RequiredArgsConstructor
public class TossPaymentService {

	private final RestClient tossRestClient;

	public TossPaymentResponse confirmPayment(PaymentConfirmRequest request) {
		return tossRestClient.post()
			.uri("/v1/payments/confirm")
			.body(Map.of(
				"paymentKey", request.paymentKey(),
				"orderId", request.orderId(),
				"amount", request.amount()
			))
			.retrieve()
			.body(TossPaymentResponse.class);
	}
}
