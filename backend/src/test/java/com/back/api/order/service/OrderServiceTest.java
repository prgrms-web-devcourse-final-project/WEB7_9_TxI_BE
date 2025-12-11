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
import com.back.api.payment.order.service.OrderService;
import com.back.api.seat.service.SeatService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.Order;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
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
	private SeatService seatService;

	@Test
	@DisplayName("주문 생성 성공")
	void createOrder_Success() {
		// given
		Long amount = 25000L;
		Long eventId = 7L;
		Long userId = 5L;
		Long seatId = 102L;

		OrderRequestDto requestDto = new OrderRequestDto(amount, eventId, userId, seatId);

		Event mockEvent = mock(Event.class);
		User mockUser = mock(User.class);
		Seat mockSeat = mock(Seat.class);

		given(eventRepository.getReferenceById(eventId)).willReturn(mockEvent);
		given(userRepository.getReferenceById(userId)).willReturn(mockUser);
		given(seatRepository.getReferenceById(seatId)).willReturn(mockSeat);

		Order savedOrder = Order.builder()
			.id(1L)
			.amount(amount)
			.event(mockEvent)
			.user(mockUser)
			.seat(mockSeat)
			.status(OrderStatus.PAID)
			.build();

		given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

		// confirmPurchase는 Seat을 반환
		given(seatService.confirmPurchase(eventId, seatId, userId)).willReturn(mockSeat);

		// when
		Order result = orderService.createOrder(requestDto);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getAmount()).isEqualTo(amount);
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getEvent()).isEqualTo(mockEvent);
		assertThat(result.getUser()).isEqualTo(mockUser);
		assertThat(result.getSeat()).isEqualTo(mockSeat);

		// verify
		verify(eventRepository, times(1)).getReferenceById(eventId);
		verify(userRepository, times(1)).getReferenceById(userId);
		verify(seatRepository, times(1)).getReferenceById(seatId);
		verify(orderRepository, times(1)).save(any(Order.class));
		verify(seatService, times(1)).confirmPurchase(eventId, seatId, userId);
	}

	@Test
	@DisplayName("주문 생성 시 올바른 Order 객체가 저장되는지 검증")
	void createOrder_VerifyOrderObject() {
		// given
		OrderRequestDto requestDto = new OrderRequestDto(30000L, 10L, 20L, 200L);

		Event mockEvent = mock(Event.class);
		User mockUser = mock(User.class);
		Seat mockSeat = mock(Seat.class);

		given(eventRepository.getReferenceById(anyLong())).willReturn(mockEvent);
		given(userRepository.getReferenceById(anyLong())).willReturn(mockUser);
		given(seatRepository.getReferenceById(anyLong())).willReturn(mockSeat);
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		orderService.createOrder(requestDto);

		// then
		verify(orderRepository).save(argThat(order ->
			order.getAmount().equals(30000L)
				&& order.getStatus() == OrderStatus.PAID
				&& order.getEvent() == mockEvent
				&& order.getUser() == mockUser
				&& order.getSeat() == mockSeat
		));
	}

	@Test
	@DisplayName("주문 생성 후 좌석 구매 확정 서비스가 호출되는지 검증")
	void createOrder_CallsSeatServiceConfirmPurchase() {
		// given
		Long eventId = 1L;
		Long userId = 2L;
		Long seatId = 3L;

		OrderRequestDto requestDto = new OrderRequestDto(10000L, eventId, userId, seatId);

		given(eventRepository.getReferenceById(anyLong())).willReturn(mock(Event.class));
		given(userRepository.getReferenceById(anyLong())).willReturn(mock(User.class));
		given(seatRepository.getReferenceById(anyLong())).willReturn(mock(Seat.class));
		given(orderRepository.save(any(Order.class))).willReturn(mock(Order.class));

		// confirmPurchase Mock 추가
		given(seatService.confirmPurchase(anyLong(), anyLong(), anyLong())).willReturn(mock(Seat.class));

		// when
		orderService.createOrder(requestDto);

		// then
		verify(seatService, times(1)).confirmPurchase(eventId, seatId, userId);
	}
}
