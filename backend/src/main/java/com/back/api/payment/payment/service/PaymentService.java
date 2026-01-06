package com.back.api.payment.payment.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.service.OrderService;
import com.back.api.payment.payment.client.PaymentClient;
import com.back.api.payment.payment.dto.request.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.request.V2_PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentConfirmResult;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;
import com.back.api.payment.payment.dto.response.V2_PaymentConfirmResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.notification.systemMessage.NotificationMessage;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.payment.entity.ApproveStatus;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.observability.metrics.BusinessMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

	private final OrderService orderService;
	private final PaymentClient paymentClient;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final ApplicationEventPublisher eventPublisher;
	private final TossPaymentService tossPaymentService;
	private final PaymentTransactionService paymentTransactionService;
	private final BusinessMetrics businessMetrics;

	@Transactional
	public PaymentReceiptResponse confirmPayment(
		Long orderId,
		String clientPaymentKey,
		Long clientAmount,
		Long userId
	) {

		// OrderService가 order의 정합성(주문자/주문상태/amount) 보장
		Order order = orderService.getOrderForPayment(orderId, userId, clientAmount);

		PaymentConfirmResult result = paymentClient.confirm(
			new PaymentConfirmCommand(
				order.getId(),
				order.getOrderKey(),
				order.getAmount()
			)
		);

		if (!result.success()) {
			order.markFailed();
			ticketService.failPayment(order.getTicket().getId()); // Ticket FAILED + Seat 해제
			businessMetrics.paymentConfirmFailure("PAYMENT_FAILED");
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		// PG사에서 받은 paymentKey와 클라이언트가 보낸 paymentKey 일치 여부 검증
		if (!result.paymentKey().equals(clientPaymentKey)) {
			businessMetrics.paymentConfirmFailure("PAYMENT_KEY_MISMATCH");
			throw new ErrorException(PaymentErrorCode.PAYMENT_KEY_MISMATCH);
		}

		// Order 성공
		order.markPaid(result.paymentKey());

		// Ticket 발급
		Ticket ticket = ticketService.confirmPayment(
			order.getTicket().getId(),
			userId
		);

		// 결제 성공 메트릭
		businessMetrics.paymentConfirmSuccess(ticket.getEvent().getId());

		// Queue 완료
		queueEntryProcessService.completePayment(
			ticket.getEvent().getId(),
			userId
		);

		String eventTitle = ticket.getEvent().getTitle();

		// 알림 메시지 발행
		eventPublisher.publishEvent(
			NotificationMessage.paymentSuccess(
				userId,
				eventTitle,
				order.getAmount()
			)
		);

		// 최종 결과조회
		// mock구성에서는 프론트 개발속도를 위해 confirmPayment에서 처리
		// PG연동시에 분리 필요
		order = orderService.getOrderWithDetails(orderId, userId);
		ticket = order.getTicket();

		return PaymentReceiptResponse.from(order, ticket);
	}

	/**
	 * 결제 영수증 조회
	 * - 결제 완료 화면에 필요한 모든 정보 제공
	 */
	@Transactional(readOnly = true)
	public PaymentReceiptResponse getPaymentReceipt(Long orderId, Long userId) {
		Order order = orderService.getOrderWithDetails(orderId, userId);
		Ticket ticket = order.getTicket();

		return PaymentReceiptResponse.from(order, ticket);
	}

	/**
	 * V2 결제 승인 - PG 호출은 트랜잭션 밖에서 처리
	 *
	 * 트랜잭션 분리 이유:
	 * - PG API 호출 중 DB 커넥션 점유 방지
	 * - 외부 API 타임아웃 시 트랜잭션 롤백 문제 방지
	 *
	 * 처리 흐름:
	 * 0. 멱등성 확인 (이미 결제 완료된 주문이면 기존 결과 반환)
	 * 1. Order 검증 (읽기 트랜잭션)
	 * 2. PG API 호출 (트랜잭션 밖)
	 * 3. 성공/실패 DB 처리 (각각 단일 쓰기 트랜잭션 - PaymentTransactionService)
	 */
	public V2_PaymentConfirmResponse v2_confirmPayment(
		String orderId,
		String paymentKey,
		Long clientAmount,
		Long userId
	) {
		// 0. 멱등성 확인: 이미 결제 완료된 주문이면 기존 결과 반환
		Optional<V2_Order> paidOrder = orderService.v2_findPaidOrder(orderId, userId);
		if (paidOrder.isPresent()) {
			log.info("[Payment] 이미 결제 완료된 주문 - orderId: {}", orderId);
			// 간단한 응답 반환 (전체 데이터 조회 불필요)
			return new V2_PaymentConfirmResponse(orderId, true);
		}

		// 1. Order 검증 (읽기 트랜잭션)
		Long ticketId = orderService.v2_validateAndGetTicketId(orderId, userId, clientAmount);

		// 2. PG 호출 (트랜잭션 밖)
		V2_PaymentConfirmRequest request = new V2_PaymentConfirmRequest(orderId, paymentKey, clientAmount);
		TossPaymentResponse result = tossPaymentService.confirmPayment(request);

		// 3. 결과에 따라 분기 - PaymentTransactionService에서 단일 트랜잭션으로 처리
		if (result.status() != ApproveStatus.DONE) {
			paymentTransactionService.handleFailure(orderId, ticketId);
			businessMetrics.paymentConfirmFailure("TOSS_PAYMENT_NOT_DONE");
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		return paymentTransactionService.handleSuccess(orderId, result, userId);
	}
}
