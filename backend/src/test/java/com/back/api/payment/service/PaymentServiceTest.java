package com.back.api.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.api.payment.order.service.OrderService;
import com.back.api.payment.payment.client.PaymentClient;
import com.back.api.payment.payment.dto.response.PaymentConfirmResult;
import com.back.api.payment.payment.dto.response.PaymentReceiptResponse;
import com.back.api.payment.payment.service.PaymentService;
import com.back.api.queue.service.QueueEntryProcessService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.factory.OrderFactory;
import com.back.support.factory.SeatFactory;
import com.back.support.factory.StoreFactory;
import com.back.support.factory.TicketFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

	@InjectMocks
	private PaymentService paymentService;

	@Mock
	private OrderService orderService;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private TicketService ticketService;

	@Mock
	private QueueEntryProcessService queueEntryProcessService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private final Store store = StoreFactory.fakeStore(1L);

	@Test
	@DisplayName("결제 성공 - Order PAID, Ticket ISSUED, Seat SOLD")
	void confirmPayment_success() {
		// given
		Long userId = 100L;
		String paymentKey = "test_payment_key";
		Long amount = 50_000L;

		var user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
		var event = EventFactory.fakeEvent(store);
		var seat = SeatFactory.fakeSeat(event, "A1", SeatGrade.VIP, 50_000);
		var draftTicket = TicketFactory.fakeDraftTicket(user, seat, event);
		var order = OrderFactory.fakePendingOrder(draftTicket, amount);

		var issuedTicket = TicketFactory.fakeIssuedTicket(user, seat, event);
		// 결제 완료 후 상태 (PAID + issuedTicket)
		var paidOrder = OrderFactory.fakePaidOrder(issuedTicket, amount, paymentKey);

		given(orderService.getOrderForPayment(any(), any(), any()))
			.willReturn(order);

		// 영수증 조회 시 PAID 상태의 order 반환
		given(orderService.getOrderWithDetails(any(), any()))
			.willReturn(paidOrder);

		given(paymentClient.confirm(any()))
			.willReturn(new PaymentConfirmResult(paymentKey, amount, true));

		given(ticketService.confirmPayment(any(), eq(userId)))
			.willReturn(issuedTicket);

		// when
		PaymentReceiptResponse response = paymentService.confirmPayment(
			1L,
			paymentKey,
			amount,
			userId
		);

		// then
		assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(response.paymentKey()).isEqualTo(paymentKey);
		assertThat(response.ticketStatus()).isEqualTo(TicketStatus.ISSUED);

		verify(ticketService).confirmPayment(any(), eq(userId));
		verify(ticketService, never()).failPayment(any());
	}

	@Test
	@DisplayName("결제 실패 - Order FAILED, Ticket FAILED, Seat AVAILABLE")
	void confirmPayment_fail() {
		// given
		Long userId = 100L;
		String paymentKey = "test_payment_key";
		Long amount = 50_000L;

		var user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
		var event = EventFactory.fakeEvent(store);
		var seat = SeatFactory.fakeSeat(event, "A1", SeatGrade.VIP, 50_000);
		var draftTicket = TicketFactory.fakeDraftTicket(user, seat, event);
		var order = OrderFactory.fakePendingOrder(draftTicket, amount);

		given(orderService.getOrderForPayment(any(), any(), any()))
			.willReturn(order);

		given(paymentClient.confirm(any()))
			.willReturn(new PaymentConfirmResult(null, amount, false));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(
			1L,
			paymentKey,
			amount,
			userId
		))
			.isInstanceOf(ErrorException.class)
			.hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_FAILED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

		verify(ticketService).failPayment(any());
		verify(ticketService, never()).confirmPayment(any(), any());
		verify(queueEntryProcessService, never()).completePayment(any(), any());
	}
}
