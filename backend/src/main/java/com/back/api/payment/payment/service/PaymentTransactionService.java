package com.back.api.payment.payment.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.payment.dto.response.TossPaymentResponse;
import com.back.api.payment.payment.dto.response.V2_PaymentConfirmResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.notification.systemMessage.OrderSuccessV2Message;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.order.repository.V2_OrderRepository;
import com.back.domain.payment.payment.entity.Payment;
import com.back.domain.payment.payment.repository.PaymentRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.observability.metrics.BusinessMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 트랜잭션 처리 전용 서비스
 * PaymentService의 self-invocation 문제를 해결하기 위해 분리
 *
 * 설계 원칙:
 * - 성공/실패 각각 하나의 트랜잭션으로 처리
 * - PG 호출은 PaymentService에서 트랜잭션 밖에서 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {

	private final V2_OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final ApplicationEventPublisher eventPublisher;
	private final BusinessMetrics businessMetrics;

	/**
	 * 결제 실패 처리 - 단일 트랜잭션
	 * Order FAILED + Ticket FAILED + Seat 해제
	 */
	@Transactional
	public void handleFailure(String orderId, Long ticketId) {
		log.info("[Payment] 결제 실패 처리 - orderId: {}, ticketId: {}", orderId, ticketId);

		V2_Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));
		order.markFailed();

		ticketService.failPayment(ticketId);
	}

	/**
	 * 결제 성공 처리 - 단일 트랜잭션
	 * Payment 저장 + Order PAID + Ticket CONFIRM + Queue 완료 + 알림 발행
	 *
	 * 멱등성 보장: 이미 PAID 상태인 주문은 기존 결과 반환
	 */
	@Transactional
	public V2_PaymentConfirmResponse handleSuccess(
		String orderId,
		TossPaymentResponse pgResponse,
		Long userId
	) {
		// Order 조회
		V2_Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));

		// 멱등성: 이미 결제 완료된 주문이면 기존 결과 반환
		if (order.getStatus() == OrderStatus.PAID) {
			log.info("[Payment] 이미 결제 완료된 주문 - orderId: {}", orderId);
			return V2_PaymentConfirmResponse.from(order, true);
		}

		// Payment 저장
		Payment payment = paymentRepository.save(
			new Payment(
				pgResponse.paymentKey(),
				orderId,
				order.getAmount(),
				pgResponse.method(),
				pgResponse.status()
			)
		);

		// Order 결제 완료
		order.setPayment(payment);
		order.markPaid(pgResponse.paymentKey());

		// Ticket 발급
		Ticket ticket = ticketService.confirmPayment(
			order.getTicket().getId(),
			userId
		);

		// 메트릭
		businessMetrics.paymentConfirmSuccess(ticket.getEvent().getId());

		// Queue 완료
		queueEntryProcessService.completePayment(
			ticket.getEvent().getId(),
			userId
		);

		// 알림 발행
		eventPublisher.publishEvent(
			new OrderSuccessV2Message(
				userId,
				orderId,
				order.getAmount(),
				ticket.getEvent().getTitle()
			)
		);

		return V2_PaymentConfirmResponse.from(order, true);
	}
}
