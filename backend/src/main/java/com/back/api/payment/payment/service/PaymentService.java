package com.back.api.payment.payment.service;

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
import com.back.domain.notification.systemMessage.OrderSuccessMessage;
import com.back.domain.notification.systemMessage.OrderSuccessV2Message;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.payment.entity.ApproveStatus;
import com.back.domain.payment.payment.entity.Payment;
import com.back.domain.payment.payment.repository.PaymentRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

	private final OrderService orderService;
	private final PaymentClient paymentClient;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final ApplicationEventPublisher eventPublisher;
	private final PaymentRepository paymentRepository;
	private final TossPaymentService tossPaymentService;

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
			new OrderSuccessMessage(
				userId,
				orderId,
				order.getAmount(),
				eventTitle
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

	@Transactional
	public V2_PaymentConfirmResponse v2_confirmPayment(
		String orderId,
		String paymentKey,
		Long clientAmount,
		Long userId
	) {

		// OrderService가 order의 정합성(주문자/주문상태/amount) 보장
		V2_Order order = orderService.v2_getOrderForPayment(orderId, userId, clientAmount);
		log.info("[v2 결제 디버깅] - 결제 승인 메서드: 결제 서비스 로그");
		log.info("[v2 결제 디버깅] - orderId : {}", orderId);
		log.info("[v2 결제 디버깅] - paymentKey : {}", paymentKey);
		log.info("[v2 결제 디버깅] - userId : {}", userId);
		V2_PaymentConfirmRequest request = new V2_PaymentConfirmRequest(orderId, paymentKey, order.getAmount());

		TossPaymentResponse result = tossPaymentService.confirmPayment(request);

		log.info("[v2 결제 디버깅] - 결제 승인 결과 {}", result.status());

		if (result.status() != ApproveStatus.DONE) { // 결제 승인 완료시 토스 API 응답 : Status = "DONE"
			order.markFailed();
			ticketService.failPayment(order.getTicket().getId()); // Ticket FAILED + Seat 해제
			//TODO 결제 실패 로직 추가
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
		}

		//결제 엔티티 생성 및 DB 저장 (결제 정보 저장)
		Payment savedPayment = paymentRepository.save(
			new Payment(
				paymentKey,
				orderId,
				order.getAmount(),
				result.method(),
				result.status()
			)
		);

		// Order status PENDING -> PAID, paymentKey DB 저장 (주문 상태 업테이트)
		order.markPaid(result.paymentKey());

		// 테스트용 : DataInit으로 만든 테스트 데이터 사용한 결제 테스트에서 사용
		//order.getTicket().getSeat().markAsReserved();

		// Ticket 발급
		Ticket ticket = ticketService.confirmPayment(
			order.getTicket().getId(),
			userId
		);

		// Queue 완료 // 테스트 데이터로 진행 시 : 큐 대기열이 없으므로 이부분도 주석처리
		queueEntryProcessService.completePayment(
			ticket.getEvent().getId(),
			userId
		);

		String eventTitle = ticket.getEvent().getTitle();

		// 알림 메시지 발행
		eventPublisher.publishEvent(
			new OrderSuccessV2Message(
				userId,
				orderId,
				order.getAmount(),
				eventTitle
			)
		);

		return new V2_PaymentConfirmResponse(orderId, true);
	}
}
