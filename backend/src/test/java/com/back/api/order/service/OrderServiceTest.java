package com.back.api.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private TicketService ticketService;

	@Test
	@DisplayName("주문 생성 성공 - Draft Ticket 검증 후 Order 생성 및 Ticket 확정")
	void createOrder_success() {
		// given
		Long userId = 5L;
		Long eventId = 7L;
		Long seatId = 102L;
		Long amount = 25_000L;
		Long ticketId = 1L;
		Long orderId = 10L;

		OrderRequestDto requestDto = new OrderRequestDto(amount, eventId, seatId);

		Event event = mock(Event.class);
		User user = mock(User.class);
		Seat seat = mock(Seat.class);

		Ticket draftTicket = Ticket.builder()
			.id(ticketId)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		Ticket issuedTicket = Ticket.builder()
			.id(ticketId)
			.ticketStatus(TicketStatus.ISSUED)
			.build();

		Order savedOrder = Order.builder()
			.id(orderId)
			.amount(amount)
			.status(OrderStatus.PAID)
			.build();

		given(ticketService.getDraftTicket(seatId, userId)).willReturn(draftTicket);
		given(eventRepository.getReferenceById(eventId)).willReturn(event);
		given(userRepository.getReferenceById(userId)).willReturn(user);
		given(seatRepository.getReferenceById(seatId)).willReturn(seat);
		given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
		given(ticketService.confirmPayment(ticketId, userId)).willReturn(issuedTicket);

		// when
		OrderResponseDto response = orderService.createOrder(requestDto, userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.orderId()).isEqualTo(orderId);
		assertThat(response.ticketId()).isEqualTo(ticketId);
		assertThat(response.amount()).isEqualTo(amount);

		verify(ticketService).getDraftTicket(seatId, userId);
		verify(orderRepository).save(any(Order.class));
		verify(ticketService).confirmPayment(ticketId, userId);
	}

	@Test
	@DisplayName("주문 생성 시 Order 객체가 올바르게 조립되는지 검증")
	void createOrder_savesCorrectOrder() {
		// given
		Long userId = 1L;
		OrderRequestDto requestDto = new OrderRequestDto(30_000L, 10L, 20L);

		Event event = mock(Event.class);
		User user = mock(User.class);
		Seat seat = mock(Seat.class);

		Ticket draftTicket = Ticket.builder()
			.id(99L)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		given(ticketService.getDraftTicket(anyLong(), eq(userId))).willReturn(draftTicket);
		given(eventRepository.getReferenceById(anyLong())).willReturn(event);
		given(userRepository.getReferenceById(userId)).willReturn(user);
		given(seatRepository.getReferenceById(anyLong())).willReturn(seat);
		given(orderRepository.save(any(Order.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(ticketService.confirmPayment(anyLong(), eq(userId)))
			.willReturn(mock(Ticket.class));

		// when
		orderService.createOrder(requestDto, userId);

		// then
		verify(orderRepository).save(argThat(order ->
			order.getAmount().equals(30_000L)
				&& order.getStatus() == OrderStatus.PAID
				&& order.getEvent() == event
				&& order.getUser() == user
				&& order.getSeat() == seat
		));
	}

	@Test
	@DisplayName("주문 생성 시 TicketService.confirmPayment가 호출된다")
	void createOrder_callsConfirmPayment() {
		// given
		Long userId = 2L;
		Long seatId = 3L;
		Long ticketId = 100L;

		OrderRequestDto requestDto = new OrderRequestDto(10_000L, 1L, seatId);

		Ticket draftTicket = Ticket.builder()
			.id(ticketId)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		given(ticketService.getDraftTicket(seatId, userId)).willReturn(draftTicket);
		given(eventRepository.getReferenceById(anyLong())).willReturn(mock(Event.class));
		given(userRepository.getReferenceById(userId)).willReturn(mock(User.class));
		given(seatRepository.getReferenceById(anyLong())).willReturn(mock(Seat.class));
		given(orderRepository.save(any(Order.class))).willReturn(mock(Order.class));
		given(ticketService.confirmPayment(ticketId, userId)).willReturn(mock(Ticket.class));

		// when
		orderService.createOrder(requestDto, userId);

		// then
		verify(ticketService).getDraftTicket(seatId, userId);
		verify(ticketService).confirmPayment(ticketId, userId);
	}
}
