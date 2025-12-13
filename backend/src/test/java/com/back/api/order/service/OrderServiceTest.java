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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.api.payment.order.dto.request.OrderRequestDto;
import com.back.api.payment.order.dto.response.OrderResponseDto;
import com.back.api.payment.order.service.OrderService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.payment.order.repository.OrderRepository;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.factory.OrderFactory;
import com.back.support.factory.SeatFactory;
import com.back.support.factory.TicketFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private TicketService ticketService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("주문 생성 성공 - Draft Ticket 존재, amount 정상")
	void createOrder_success() {
		// given
		Long userId = 1L;
		Long eventId = 100L;
		Long seatId = 200L;
		Long amount = 50_000L;

		var event = EventFactory.fakeEvent();
		var seat = SeatFactory.fakeSeat(event, "A1", SeatGrade.VIP, 50_000);
		var user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		var draftTicket = TicketFactory.fakeDraftTicket(user, seat, event);

		given(ticketService.getDraftTicket(eventId, seatId, userId))
			.willReturn(draftTicket);

		var savedOrder = OrderFactory.fakePendingOrder(draftTicket, amount);
		given(orderRepository.save(any()))
			.willReturn(savedOrder);

		// when
		OrderResponseDto response = orderService.createOrder(
			new OrderRequestDto(amount, eventId, seatId),
			userId
		);

		// then
		assertThat(response.orderKey()).isNotNull();
		assertThat(response.amount()).isEqualTo(amount);
	}

	@Test
	@DisplayName("주문 생성 실패 - 금액 불일치")
	void createOrder_amountMismatch() {
		// given
		Long userId = 1L;
		Long eventId = 100L;
		Long seatId = 200L;
		Long wrongAmount = 30_000L;

		var event = EventFactory.fakeEvent();
		var seat = SeatFactory.fakeSeat(event, "A1", SeatGrade.VIP, 50_000);
		var user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		var draftTicket = TicketFactory.fakeDraftTicket(user, seat, event);

		given(ticketService.getDraftTicket(eventId, seatId, userId))
			.willReturn(draftTicket);

		// when & then
		assertThatThrownBy(() ->
			orderService.createOrder(
				new OrderRequestDto(wrongAmount, eventId, seatId),
				userId
			)
		)
			.isInstanceOf(ErrorException.class)
			.hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.AMOUNT_MISMATCH);

		verify(orderRepository, never()).save(any());
	}
}
