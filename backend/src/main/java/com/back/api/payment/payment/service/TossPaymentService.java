package com.back.api.payment.payment.service;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.request.V2_PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;

import lombok.RequiredArgsConstructor;


// 백엔드 <-> 토스페이먼츠 api 서버
@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentService {

	private final RestClient tossRestClient;

	public TossPaymentResponse confirmPayment(V2_PaymentConfirmRequest request) {
		log.info("====== 요청 데이터 ======");
		log.info("orderId: {}", request.orderId());
		log.info("paymentKey: {}", request.paymentKey());
		log.info("amount: {}", request.amount());

		TossPaymentResponse response = tossRestClient.post()
			.uri("/v1/payments/confirm")
			.body(Map.of(
				"paymentKey", request.paymentKey(),
				"orderId", request.orderId(),
				"amount", request.amount()
			))
			.retrieve()
			.body(TossPaymentResponse.class);

		log.info("====== 응답 데이터 ======");
		log.info("Response: {}", response);

		return response;

	}
}