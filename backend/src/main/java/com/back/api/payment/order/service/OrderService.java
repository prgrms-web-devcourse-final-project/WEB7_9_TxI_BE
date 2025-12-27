package com.back.api.payment.order.service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.dto.response.V2_OrderResponseDto;
import com.back.api.ticket.service.TicketService;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.payment.order.repository.V2_OrderRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
	private final OrderRepository orderRepository;
	private final TicketService ticketService;
	private final V2_OrderRepository v2_orderRepository;
	/**
	 * 주문 생성
	 * draft 티켓 확인 -> 주문 생성 -> 티켓 상태 PAID로 변경
	 */
	@Transactional
	public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, Long userId) {

		// 티켓이 DRAFT 상태인지 확인
		Ticket draft = ticketService.getDraftTicket(orderRequestDto.eventId(), orderRequestDto.seatId(), userId);

		// 이미 PENDING 상태의 Order가 있는지 확인
		// 다중 요청 방어 로직
		Optional<Order> existingOrder = orderRepository.findByTicketIdAndStatus(
			draft.getId(),
			OrderStatus.PENDING
		);

		if (existingOrder.isPresent()) {
			// 기존 Order 재사용 (새로 만들지 않음)
			log.info("Duplicate order request, reusing existing orderId={}", existingOrder.get().getId());
			return OrderResponseDto.from(existingOrder.get(), draft);
		}

		// 금액 일치 여부 확인
		Integer actualAmount = draft.getSeat().getPrice();
		if (!orderRequestDto.amount().equals(actualAmount.longValue())) {
			throw new ErrorException(OrderErrorCode.AMOUNT_MISMATCH);
		}

		// 주문번호 생성 (WF + 10자리 숫자)
		String orderNumber = generateOrderNumber();

		// 주문 생성
		Order newOrder = Order.builder()
			.ticket(draft)
			.amount(orderRequestDto.amount())
			.status(OrderStatus.PENDING)
			.orderKey(UUID.randomUUID().toString())
			.orderNumber(orderNumber)
			.build();

		Order savedOrder = orderRepository.save(newOrder);

		return OrderResponseDto.from(savedOrder, savedOrder.getTicket());
	}

	/**
	 * 주문번호 생성 (WF + 10자리 숫자)
	 */
	private String generateOrderNumber() {
		Random random = new Random();
		long number = 1000000000L + (long)(random.nextDouble() * 9000000000L);
		return "WF" + number;
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

	/**
	 * 영수증 조회용 Order 조회 (Ticket, Event, Seat 포함)
	 */
	@Transactional(readOnly = true)
	public Order getOrderWithDetails(Long orderId, Long userId) {
		Order order = orderRepository.findByIdWithDetails(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));

		if (!order.getTicket().getOwner().getId().equals(userId)) {
			throw new ErrorException(OrderErrorCode.UNAUTHORIZED_ORDER_ACCESS);
		}

		return order;
	}

	@Transactional
	public V2_OrderResponseDto v2_createOrder(OrderRequestDto orderRequestDto, Long userId) {

		// 티켓이 DRAFT 상태인지 확인
		Ticket draft = ticketService.getDraftTicket(orderRequestDto.eventId(), orderRequestDto.seatId(), userId);

		// 금액 일치 여부 확인
		Integer actualAmount = draft.getSeat().getPrice();
		if (!orderRequestDto.amount().equals(actualAmount.longValue())) {
			throw new ErrorException(OrderErrorCode.AMOUNT_MISMATCH);
		}

		// 주문 생성
		V2_Order newOrder = V2_Order.builder()
			.ticket(draft)
			.amount(actualAmount.longValue())
			.status(OrderStatus.PENDING)
			.build();

		V2_Order savedOrder = v2_orderRepository.save(newOrder);

		return V2_OrderResponseDto.from(savedOrder);
	}

	// 결제 가능한 Order 조회 및 검증 -> 결제 서비스에 보장
	@Transactional(readOnly = true)
	public V2_Order v2_getOrderForPayment(String orderId, Long userId, Long clientAmount) {

		V2_Order order = v2_orderRepository.findById(orderId)
			.orElseThrow(() -> new ErrorException(OrderErrorCode.ORDER_NOT_FOUND));

		if (!order.getTicket().getOwner().getId().equals(userId)) {
			throw new ErrorException(OrderErrorCode.UNAUTHORIZED_ORDER_ACCESS);
		}

		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ErrorException(OrderErrorCode.INVALID_ORDER_STATUS);
		}

		if (!order.getAmount().equals(clientAmount)) {
			//TODO 금액 불일치 로직 : paymentService.amountNotEqual()
			throw new ErrorException(PaymentErrorCode.AMOUNT_VERIFICATION_FAILED);
		}

		return order;
	}
}
