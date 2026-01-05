package com.back.api.payment.payment.service;

import java.util.Map;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss Payments API 연동 서비스
 * 장애 대응 전략:
 * - 서킷브레이커: 10개 요청 중 50% 실패 시 OPEN → 30초 후 HALF_OPEN
 * - HTTP 타임아웃: 연결 3초, 읽기 5초 (TossPaymentConfig에서 설정)
 * - Retry 미적용: 결제 API는 중복 호출 시 이중 결제 위험
 *
 * 멱등성:
 * Toss API는 동일 paymentKey로 재호출 시 기존 결과 반환 (멱등성 보장)
 * 하지만 네트워크 타임아웃 등으로 응답을 못 받은 경우 클라이언트 측에서
 * 결제 상태 조회 API로 확인 후 재시도하는 것이 안전함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentService {

	private static final String CIRCUIT_BREAKER_NAME = "tossPayment";

	private final RestClient tossRestClient;

	/**
	 * Toss 결제 승인 API 호출
	 *
	 * @param request 결제 승인 요청 (orderId, paymentKey, amount)
	 * @return 결제 승인 결과
	 * @throws ErrorException 서킷브레이커 OPEN, 연결 실패, API 오류 시
	 */
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "handleFailure")
	public TossPaymentResponse confirmPayment(V2_PaymentConfirmRequest request) {
		// log.info("[Toss] 결제 승인 요청 - orderId: {}, amount: {}", request.orderId(), request.amount());

		TossPaymentResponse response = tossRestClient.post()
			.uri("/v1/payments/confirm")
			.body(Map.of(
				"paymentKey", request.paymentKey(),
				"orderId", request.orderId(),
				"amount", request.amount()
			))
			.retrieve()
			.body(TossPaymentResponse.class);

		// log.info("[Toss] 결제 승인 완료 - orderId: {}, status: {}", request.orderId(), response.status());

		return response;
	}

	/**
	 * 서킷브레이커 fallback - 장애 유형별 처리
	 */
	private TossPaymentResponse handleFailure(V2_PaymentConfirmRequest request, Throwable throwable) {
		// 서킷브레이커 OPEN 상태 - PG 서비스 일시 중단
		if (throwable instanceof CallNotPermittedException) {
			log.error("[Toss] 서킷브레이커 OPEN - orderId: {}", request.orderId());
			throw new ErrorException(PaymentErrorCode.PG_UNAVAILABLE);
		}

		// 연결/타임아웃 실패 - PG 연결 불가
		if (throwable instanceof ResourceAccessException) {
			log.error("[Toss] 연결/타임아웃 실패 - orderId: {}, error: {}",
				request.orderId(), throwable.getMessage());
			throw new ErrorException(PaymentErrorCode.PG_TIMEOUT);
		}

		// REST API 오류 (4xx, 5xx 등)
		if (throwable instanceof RestClientException) {
			log.error("[Toss] API 오류 - orderId: {}, error: {}",
				request.orderId(), throwable.getMessage());
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		// 예상치 못한 오류
		log.error("[Toss] 알 수 없는 오류 - orderId: {}", request.orderId(), throwable);
		throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
	}
}