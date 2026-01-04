package com.back.api.payment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.payment.payment.dto.request.V2_PaymentConfirmRequest;
import com.back.api.payment.payment.dto.response.TossPaymentResponse;
import com.back.api.payment.payment.dto.response.V2_PaymentConfirmResponse;
import com.back.api.payment.payment.service.PaymentService;
import com.back.api.payment.payment.service.TossPaymentService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.entity.V2_Order;
import com.back.domain.payment.order.repository.V2_OrderRepository;
import com.back.domain.payment.payment.entity.ApproveStatus;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.OrderErrorCode;
import com.back.global.error.code.PaymentErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.helper.OrderHelper;
import com.back.support.helper.QueueEntryHelper;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

/**
 * V2 결제 서비스 통합 테스트
 *
 * 테스트 원칙:
 * - 구현 디테일이 아닌 비즈니스 동작(behavior)을 검증
 * - 최종 상태와 결과값에 집중
 * - 내부 메서드 호출 순서나 횟수는 검증하지 않음
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("V2 PaymentService 통합 테스트")
class V2PaymentServiceIntegrationTest {

	@TestConfiguration
	static class TestConfig {
		/**
		 * PG 서비스 Fake 구현
		 * Mock 대신 Fake를 사용하여 테스트 가독성 향상
		 */
		@Bean
		@Primary
		public TossPaymentService fakeTossPaymentService() {
			return new FakeTossPaymentService();
		}
	}

	/**
	 * Fake PG 서비스 - 테스트 시나리오에 따라 동작 변경 가능
	 */
	static class FakeTossPaymentService extends TossPaymentService {
		private TossPaymentResponse nextResponse;
		private ErrorException nextException;

		public FakeTossPaymentService() {
			super(null); // RestClient는 사용하지 않음
		}

		public void willReturn(TossPaymentResponse response) {
			this.nextResponse = response;
			this.nextException = null;
		}

		public void willThrow(ErrorException exception) {
			this.nextException = exception;
			this.nextResponse = null;
		}

		public void reset() {
			this.nextResponse = null;
			this.nextException = null;
		}

		@Override
		public TossPaymentResponse confirmPayment(V2_PaymentConfirmRequest request) {
			if (nextException != null) {
				throw nextException;
			}
			if (nextResponse != null) {
				return nextResponse;
			}
			// 기본값: 성공 응답
			return new TossPaymentResponse(
				"default_payment_key",
				ApproveStatus.DONE,
				"카드",
				request.amount(),
				"2024-01-01T12:00:00+09:00"
			);
		}
	}

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private FakeTossPaymentService fakePgService;

	@Autowired
	private V2_OrderRepository orderRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private SeatHelper seatHelper;

	@Autowired
	private TicketHelper ticketHelper;

	@Autowired
	private OrderHelper orderHelper;

	@Autowired
	private QueueEntryHelper queueEntryHelper;

	private User user;
	private Event event;
	private Seat seat;

	private static final Long SEAT_PRICE = 50_000L;
	private static final String PAYMENT_KEY = "test_payment_key_12345";

	@BeforeEach
	void setUp() {
		fakePgService.reset();

		Store store = storeHelper.createStore();
		user = userHelper.createUser(UserRole.NORMAL, null).user();
		event = eventRepository.save(EventFactory.fakeEvent(store, "V2 결제 테스트 이벤트"));
		seat = seatHelper.createSeat(event, "A1", SeatGrade.VIP, SEAT_PRICE.intValue());
	}

	// ========== 테스트 헬퍼 메서드 ==========

	/**
	 * 결제 가능한 상태로 테스트 데이터 셋업
	 * - Draft Ticket + Reserved Seat + ENTERED Queue + PENDING Order
	 */
	private V2_Order givenPayableOrder() {
		Ticket draftTicket = ticketHelper.createDraftTicket(user, seat, event);
		seat.markAsReserved();
		seatRepository.save(seat);
		queueEntryHelper.createEnteredQueueEntry(event, user);
		return orderHelper.createV2PendingOrder(draftTicket, SEAT_PRICE);
	}

	/**
	 * PG 성공 응답 설정
	 */
	private void givenPgWillSucceed() {
		fakePgService.willReturn(new TossPaymentResponse(
			PAYMENT_KEY,
			ApproveStatus.DONE,
			"카드",
			SEAT_PRICE,
			"2024-01-01T12:00:00+09:00"
		));
	}

	/**
	 * PG 실패 응답 설정
	 */
	private void givenPgWillFail() {
		fakePgService.willReturn(new TossPaymentResponse(
			PAYMENT_KEY,
			ApproveStatus.ABORTED,
			"카드",
			SEAT_PRICE,
			null
		));
	}

	// ========== 테스트 케이스 ==========

	@Nested
	@DisplayName("결제 성공")
	class PaymentSuccess {

		@Test
		@DisplayName("PG 승인 성공 시 Order, Ticket, Seat 상태가 변경된다")
		void success_allStatesUpdated() {
			// given
			V2_Order order = givenPayableOrder();
			givenPgWillSucceed();

			// when
			V2_PaymentConfirmResponse response = paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			);

			// then - 응답 검증
			assertThat(response.success()).isTrue();
			assertThat(response.orderId()).isEqualTo(order.getOrderId());
			assertThat(response.amount()).isEqualTo(SEAT_PRICE);

			// then - 최종 상태 검증
			V2_Order finalOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
			assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PAID);
			assertThat(finalOrder.getPaymentKey()).isEqualTo(PAYMENT_KEY);

			Ticket finalTicket = ticketRepository.findById(order.getTicket().getId()).orElseThrow();
			assertThat(finalTicket.getTicketStatus()).isEqualTo(TicketStatus.ISSUED);

			Seat finalSeat = seatRepository.findById(seat.getId()).orElseThrow();
			assertThat(finalSeat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
		}

		@Test
		@DisplayName("결제 성공 시 Payment 엔티티가 Order에 연결된다")
		void success_paymentLinkedToOrder() {
			// given
			V2_Order order = givenPayableOrder();
			givenPgWillSucceed();

			// when
			paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			);

			// then - Payment가 Order에 연결되었는지만 확인
			// (paymentKey는 success_allStatesUpdated에서 이미 검증됨)
			V2_Order finalOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
			assertThat(finalOrder.getPayment()).isNotNull();
		}

	}

	@Nested
	@DisplayName("결제 실패 - PG 응답")
	class PaymentFailure_PgResponse {

		@Test
		@DisplayName("PG 승인 거부 시 Order, Ticket FAILED, Seat 해제")
		void pgDenied_allStatesReverted() {
			// given
			V2_Order order = givenPayableOrder();
			Long ticketId = order.getTicket().getId();
			givenPgWillFail();

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_FAILED);
				});

			// then - 최종 상태 검증
			V2_Order finalOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
			assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.FAILED);

			Ticket finalTicket = ticketRepository.findById(ticketId).orElseThrow();
			assertThat(finalTicket.getTicketStatus()).isEqualTo(TicketStatus.FAILED);

			Seat finalSeat = seatRepository.findById(seat.getId()).orElseThrow();
			assertThat(finalSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
		}

		@Test
		@DisplayName("PG 서비스 불가 시 예외 발생, Order 상태 유지")
		void pgUnavailable_orderUnchanged() {
			// given
			V2_Order order = givenPayableOrder();
			fakePgService.willThrow(new ErrorException(PaymentErrorCode.PG_UNAVAILABLE));

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(PaymentErrorCode.PG_UNAVAILABLE);
				});

			// then - Order 상태 변경 없음
			V2_Order finalOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
			assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		}

		@Test
		@DisplayName("PG 타임아웃 시 예외 발생, Order 상태 유지")
		void pgTimeout_orderUnchanged() {
			// given
			V2_Order order = givenPayableOrder();
			fakePgService.willThrow(new ErrorException(PaymentErrorCode.PG_TIMEOUT));

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(PaymentErrorCode.PG_TIMEOUT);
				});

			V2_Order finalOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
			assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("결제 실패 - 주문 검증")
	class PaymentFailure_OrderValidation {

		@Test
		@DisplayName("존재하지 않는 Order ID로 요청 시 예외")
		void orderNotFound() {
			// given
			String invalidOrderId = "non-existent-order-id";

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				invalidOrderId,
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
				});
		}

		@Test
		@DisplayName("다른 사용자의 Order에 접근 시 예외")
		void unauthorizedAccess() {
			// given
			Ticket draftTicket = ticketHelper.createDraftTicket(user, seat, event);
			V2_Order order = orderHelper.createV2PendingOrder(draftTicket, SEAT_PRICE);
			User otherUser = userHelper.createUser(UserRole.NORMAL, null).user();

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				otherUser.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(OrderErrorCode.UNAUTHORIZED_ORDER_ACCESS);
				});
		}

		@Test
		@DisplayName("요청 금액이 Order 금액과 다르면 예외")
		void amountMismatch() {
			// given
			Ticket draftTicket = ticketHelper.createDraftTicket(user, seat, event);
			V2_Order order = orderHelper.createV2PendingOrder(draftTicket, SEAT_PRICE);
			Long wrongAmount = SEAT_PRICE + 10_000L;

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				order.getOrderId(),
				PAYMENT_KEY,
				wrongAmount,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(PaymentErrorCode.AMOUNT_VERIFICATION_FAILED);
				});
		}

		@Test
		@DisplayName("이미 결제 완료된 Order로 재요청 시 기존 결과 반환 (멱등성)")
		void alreadyPaid_returnsExistingResult() {
			// given - 이미 결제 완료된 Order
			Ticket issuedTicket = ticketHelper.createIssuedTicket(user, seat, event);
			V2_Order paidOrder = orderHelper.createV2PaidOrder(issuedTicket, SEAT_PRICE, "existing_key");

			// when - 동일 요청 재시도
			V2_PaymentConfirmResponse response = paymentService.v2_confirmPayment(
				paidOrder.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			);

			// then - 에러 없이 기존 성공 결과 반환 (멱등성 보장)
			assertThat(response.success()).isTrue();
			assertThat(response.orderId()).isEqualTo(paidOrder.getOrderId());
		}

		@Test
		@DisplayName("실패한 Order로 재시도 시 예외")
		void failedOrderRetry() {
			// given
			Ticket draftTicket = ticketHelper.createDraftTicket(user, seat, event);
			V2_Order failedOrder = orderHelper.createV2FailedOrder(draftTicket, SEAT_PRICE);

			// when & then
			assertThatThrownBy(() -> paymentService.v2_confirmPayment(
				failedOrder.getOrderId(),
				PAYMENT_KEY,
				SEAT_PRICE,
				user.getId()
			))
				.isInstanceOf(ErrorException.class)
				.satisfies(e -> {
					ErrorException ex = (ErrorException) e;
					assertThat(ex.getErrorCode()).isEqualTo(OrderErrorCode.INVALID_ORDER_STATUS);
				});
		}
	}
}
