package com.back.api.payment.order.service;

import org.springframework.stereotype.Service;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.seat.service.SeatService;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final SeatRepository seatRepository;
	private final SeatService seatService;

	public Order createOrder(OrderRequestDto orderRequestDto) {
		Order newOrder = Order.builder()
			.amount(orderRequestDto.amount())
			.event(eventRepository.getReferenceById(orderRequestDto.eventId()))
			.user(userRepository.getReferenceById(orderRequestDto.userId()))
			.seat(seatRepository.getReferenceById(orderRequestDto.seatId()))
			.status(OrderStatus.PAID)
			.build();
		orderRepository.save(newOrder);
		seatService.confirmPurchase(orderRequestDto.eventId(), orderRequestDto.seatId(), orderRequestDto.userId());
		// Todo : 티켓 생성 서비스 호출
		return newOrder;
	}
}
