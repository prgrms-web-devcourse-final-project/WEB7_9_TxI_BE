package com.back.api.payment.order.service;

import org.springframework.stereotype.Service;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.ticket.service.TicketService;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final SeatRepository seatRepository;
	private final TicketService ticketService;

	/**
	 * 주문 생성
	 * draft 티켓 확인 -> 주문 생성 -> 티켓 상태 PAID로 변경
	 */
	public Order createOrder(OrderRequestDto orderRequestDto) {

		// 티켓이 DRAFT 상태인지 확인
		Ticket draft = ticketService.getDraftTicket(orderRequestDto.seatId(), orderRequestDto.userId());

		// 주문 생성
		Order newOrder = Order.builder()
			.amount(orderRequestDto.amount())
			.event(eventRepository.getReferenceById(orderRequestDto.eventId()))
			.user(userRepository.getReferenceById(orderRequestDto.userId()))
			.seat(seatRepository.getReferenceById(orderRequestDto.seatId()))
			.status(OrderStatus.PAID)
			.build();
		orderRepository.save(newOrder);

		// 티켓 상태를 PAID로 변경
		ticketService.confirmPayment(draft.getId(), orderRequestDto.userId());

		return newOrder;
	}
}
