package com.back.api.payment.order.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final SeatRepository seatRepository;
	private final TicketService ticketService;
	private final QueueEntryProcessService queueEntryProcessService;

	/**
	 * 주문 생성
	 * draft 티켓 확인 -> 주문 생성 -> 티켓 상태 PAID로 변경
	 */
	@Transactional
	public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, Long userId) {

		// 티켓이 DRAFT 상태인지 확인
		Ticket draft = ticketService.getDraftTicket(orderRequestDto.eventId(), orderRequestDto.seatId(), userId);

		// 금액 일치 여부 확인
		Integer actualAmount = draft.getSeat().getPrice();
		if (!orderRequestDto.amount().equals(actualAmount.longValue())) {
			throw new ErrorException(OrderErrorCode.AMOUNT_MISMATCH);
		}

		// 주문 생성
		Order newOrder = Order.builder()
			.ticket(draft)
			.amount(orderRequestDto.amount())
			.status(OrderStatus.PENDING)
			.orderKey(UUID.randomUUID().toString())
			.build();

		Order savedOrder = orderRepository.save(newOrder);

		return OrderResponseDto.from(savedOrder, savedOrder.getTicket());
	}

	// 결제 가능한 Order 조회 및 검증 -> 결제 서비스에 보장
	@Transactional(readOnly = true)
	public Order getOrderForPayment(Long orderId, Long userId, Long clientAmount) {

		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));

		if (!order.getTicket().getOwner().getId().equals(userId)) {
			throw new ErrorException(OrderErrorCode.UNAUTHORIZED_ORDER_ACCESS);
		}

		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ErrorException(OrderErrorCode.INVALID_ORDER_STATUS);
		}

		if (!order.getAmount().equals(clientAmount)) {
			throw new ErrorException(PaymentErrorCode.AMOUNT_VERIFICATION_FAILED);
		}

		return order;
	}
}
