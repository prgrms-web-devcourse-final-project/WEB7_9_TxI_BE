package com.back.api.payment.payment.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.service.OrderService;
import com.back.api.payment.payment.client.PaymentClient;
import com.back.api.payment.payment.dto.request.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.response.PaymentConfirmResponse;
import com.back.api.payment.payment.dto.response.PaymentConfirmResult;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.notification.systemMessage.OrdersSuccessMessage;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

/**
 * Payment 관련 비즈니스 로직 처리
 * TODO: PG 연동 시 트랜잭션 경계 재설정 필요
 *
 * [설계 방향]
 * 1. PG API 호출: 트랜잭션 밖으로 이동
 * 2. 핵심 DB 변경만 @Transactional
 * 3. Queue/Notification: @TransactionalEventListener(AFTER_COMMIT)
 *
 * [리팩토링 시점]
 * - 실제 PG 연동 구현 시 (TossPaymentClient 등)
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final OrderService orderService;
	private final PaymentClient paymentClient;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public PaymentConfirmResponse confirmPayment(
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
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		// PG사에서 받은 paymentKey와 클라이언트가 보낸 paymentKey 일치 여부 검증
		if (!result.paymentKey().equals(clientPaymentKey)) {
			throw new ErrorException(PaymentErrorCode.PAYMENT_KEY_MISMATCH);
		}

		// Order 성공
		order.markPaid(result.paymentKey());

		// Ticket 발급
		Ticket ticket = ticketService.confirmPayment(
			order.getTicket().getId(),
			userId
		);

		// Queue 완료
		queueEntryProcessService.completePayment(
			ticket.getEvent().getId(),
			userId
		);

		String eventTitle = ticket.getEvent().getTitle();

		// 알림 메시지 발행
		eventPublisher.publishEvent(
			new OrdersSuccessMessage(
				userId,
				orderId,
				order.getAmount(),
				eventTitle
			)
		);

		return PaymentConfirmResponse.from(order, ticket);
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
}
