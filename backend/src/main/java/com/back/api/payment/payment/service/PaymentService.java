package com.back.api.payment.payment.service;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.service.OrderService;
import com.back.api.payment.payment.dto.request.PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.PaymentConfirmResponse;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.payment.entity.ApproveStatus;
import com.back.domain.payment.payment.entity.Payment;
import com.back.domain.payment.payment.repository.PaymentRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

/**
 * Payment 관련 비즈니스 로직 처리 ( 클라이언트 <-> 백엔드 )
 * 큐,좌석,티켓의 상태변화 과도하게 책임 -> 추후 리팩토링 필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final OrderService orderService;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;
	private final ApplicationEventPublisher eventPublisher;
	private final TossPaymentService tossPaymentService;
	private final PaymentRepository paymentRepository;


	@Transactional
	public PaymentConfirmResponse confirmPayment(
		String orderId,
		String paymentKey,
		Long clientAmount,
		Long userId
	) {

		// OrderService가 order의 정합성(주문자/주문상태/amount) 보장
		Order order = orderService.getOrderForPayment(orderId, userId, clientAmount);
		log.info("결제 승인 메서드: 결제 서비스 로그");
		log.info("orderId : {}", orderId);
		log.info("paymentKey : {}", paymentKey);
		log.info("userId : {}", userId);
		PaymentConfirmRequest request = new PaymentConfirmRequest(orderId, paymentKey, order.getAmount());

		TossPaymentResponse result = tossPaymentService.confirmPayment(request);

		if (!result.status().equals("DONE")) { // 결제 승인 완료시 토스 API 응답 : Status = "DONE"
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
				ApproveStatus.DONE
			)
		);

		// Order status PENDING -> PAID, paymentKey DB 저장 (주문 상태 업테이트)
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

		// String eventTitle = ticket.getEvent().getTitle();
		//
		// // 알림 메시지 발행
		// eventPublisher.publishEvent(
		// 	new OrdersSuccessMessage(
		// 		userId,
		// 		orderId,
		// 		order.getAmount(),
		// 		eventTitle
		// 	)
		// );

		return new PaymentConfirmResponse(orderId, true);
	}
}
