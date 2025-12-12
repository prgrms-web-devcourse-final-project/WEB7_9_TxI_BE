package com.back.api.payment.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.payment.client.PaymentClient;
import com.back.api.payment.payment.dto.PaymentConfirmCommand;
import com.back.api.payment.payment.dto.PaymentConfirmResult;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final OrderRepository orderRepository;
	private final PaymentClient paymentClient;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;

	@Transactional
	public OrderResponseDto confirmPayment(Long orderId, Long userId) {

		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));

		PaymentConfirmResult result = paymentClient.confirm(
			new PaymentConfirmCommand(
				order.getId(),
				order.getOrderKey(),
				order.getAmount()
			)
		);

		if (!result.success()) {
			order.markFailed();
			throw new ErrorException(PaymentErrorCode.PAYMENT_FAILED);
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

		return OrderResponseDto.from(order, ticket);
	}
}
