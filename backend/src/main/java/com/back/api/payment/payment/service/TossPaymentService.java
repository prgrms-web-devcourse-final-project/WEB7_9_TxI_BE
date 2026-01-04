package com.back.api.payment.payment.service;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.back.api.payment.payment.dto.request.V2_PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss Payments API 연동 서비스
 *
 * <p>서킷브레이커 설정:
 * - 10개 요청 중 50% 실패 시 OPEN
 * - OPEN 상태에서 30초 후 HALF_OPEN
 * - 타임아웃: 5초
 * - 재시도: 최대 2회 (500ms 간격)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentService {

	private static final String CIRCUIT_BREAKER_NAME = "tossPayment";

	private final RestClient tossRestClient;

	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "handlePaymentFailure")
	@Retry(name = CIRCUIT_BREAKER_NAME)
	@TimeLimiter(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "handleTimeout")
	public TossPaymentResponse confirmPayment(V2_PaymentConfirmRequest request) {
		log.info("[Toss] 결제 승인 요청 - orderId: {}, amount: {}", request.orderId(), request.amount());

		TossPaymentResponse response = tossRestClient.post()
			.uri("/v1/payments/confirm")
			.body(Map.of(
				"paymentKey", request.paymentKey(),
				"orderId", request.orderId(),
				"amount", request.amount()
			))
			.retrieve()
			.body(TossPaymentResponse.class);

		log.info("[Toss] 결제 승인 완료 - orderId: {}, status: {}", request.orderId(), response.status());

		return response;
	}

	/**
	 * 서킷브레이커 OPEN 또는 일반 예외 발생 시 폴백
	 */
	private TossPaymentResponse handlePaymentFailure(
		V2_PaymentConfirmRequest request,
		Throwable throwable
	) {
		if (throwable instanceof CallNotPermittedException) {
			log.error("[Toss] 서킷브레이커 OPEN - 결제 서비스 일시 중단. orderId: {}", request.orderId());
			throw new ErrorException(PaymentErrorCode.PG_UNAVAILABLE);
		}

		if (throwable instanceof ResourceAccessException) {
			log.error("[Toss] 연결 실패 - orderId: {}, error: {}", request.orderId(), throwable.getMessage());
			throw new ErrorException(PaymentErrorCode.PG_UNAVAILABLE);
		}

		if (throwable instanceof RestClientException) {
			log.error("[Toss] API 호출 실패 - orderId: {}, error: {}", request.orderId(), throwable.getMessage());
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		log.error("[Toss] 예상치 못한 오류 - orderId: {}", request.orderId(), throwable);
		throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
	}

	/**
	 * 타임아웃 발생 시 폴백
	 */
	private TossPaymentResponse handleTimeout(
		V2_PaymentConfirmRequest request,
		TimeoutException e
	) {
		log.error("[Toss] 타임아웃 - orderId: {}, timeout: 5s", request.orderId());
		throw new ErrorException(PaymentErrorCode.PG_TIMEOUT);
	}
}