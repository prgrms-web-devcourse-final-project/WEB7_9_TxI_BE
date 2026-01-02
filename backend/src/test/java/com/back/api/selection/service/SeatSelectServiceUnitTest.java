package com.back.api.selection.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.lock.DistributedLockManager;
import com.back.support.factory.EventFactory;
import com.back.support.factory.SeatFactory;
import com.back.support.factory.StoreFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatSelectionService 단위 테스트")
@ActiveProfiles("test")
class SeatSelectServiceUnitTest {

	@InjectMocks
	private SeatSelectionService seatSelectionService;

	@Mock
	private SeatSelectionExecutor executor;

	@Mock
	private DistributedLockManager lockManager;

	@Mock
	private PasswordEncoder passwordEncoder;

	private Event testEvent;
	private Seat testSeat;
	private User testUser;
	private Ticket testTicket;
	private Long eventId;
	private Long seatId;
	private Long userId;

	@BeforeEach
	void setUp() {
		eventId = 1L;
		seatId = 1L;
		userId = 100L;
		Store store = StoreFactory.fakeStore(1L);

		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
		testEvent = EventFactory.fakeEvent(store, "테스트 콘서트");
		testSeat = SeatFactory.fakeSeat(testEvent, "A1", SeatGrade.VIP, 150000);

		testTicket = Ticket.builder()
			.owner(testUser)
			.event(testEvent)
			.seat(testSeat)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		// DistributedLockManager mock 기본 설정: 락 획득 후 즉시 task 실행
		given(lockManager.executeWithLock(anyString(), any(Supplier.class)))
			.willAnswer(invocation -> {
				Supplier<?> task = invocation.getArgument(1);
				return task.get();
			});
	}

	@Nested
	@DisplayName("selectSeatAndCreateTicket 테스트")
	class SelectSeatAndCreateTicketTest {

		@Test
		@DisplayName("정상적으로 좌석을 선택하고 Draft Ticket을 생성/업데이트한다")
		void selectSeatAndCreateTicket_Success() {
			// given
			Ticket draftTicket = Ticket.builder()
				.owner(testUser)
				.event(testEvent)
				.seat(null)  // 좌석 없이 생성
				.ticketStatus(TicketStatus.DRAFT)
				.build();

			given(executor.selectSeat(eventId, seatId, userId)).willReturn(draftTicket);

			// when
			Ticket result = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);

			then(lockManager).should().executeWithLock(anyString(), any(Supplier.class));
			then(executor).should().selectSeat(eventId, seatId, userId);
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자는 좌석 선택에 실패한다")
		void selectSeatAndCreateTicket_NotInQueue_ThrowsException() {
			// given
			given(executor.selectSeat(eventId, seatId, userId))
				.willThrow(new ErrorException(SeatErrorCode.NOT_IN_QUEUE));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_IN_QUEUE);

			then(executor).should().selectSeat(eventId, seatId, userId);
		}

		@Test
		@DisplayName("좌석 예약 실패 시 예외가 발생한다")
		void selectSeatAndCreateTicket_ReserveFail_DoesNotCreateTicket() {
			// given
			given(executor.selectSeat(eventId, seatId, userId))
				.willThrow(new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_ALREADY_RESERVED);

			then(executor).should().selectSeat(eventId, seatId, userId);
		}
	}

	@Nested
	@DisplayName("좌석 상태 검증 테스트")
	class SeatStatusValidationTest {

		@Test
		@DisplayName("좌석 선택 성공 시 좌석이 RESERVED 상태가 된다")
		void selectSeat_SeatBecomesReserved() {
			// given
			testSeat.markAsReserved(); // 예약 상태로 변경

			Ticket draftTicket = Ticket.builder()
				.owner(testUser)
				.event(testEvent)
				.seat(testSeat)  // 좌석 할당됨
				.ticketStatus(TicketStatus.DRAFT)
				.build();

			given(executor.selectSeat(eventId, seatId, userId)).willReturn(draftTicket);

			// when
			Ticket result = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

			// then
			assertThat(result.getSeat().getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
			then(executor).should().selectSeat(eventId, seatId, userId);
		}
	}

	@Nested
	@DisplayName("트랜잭션 롤백 검증 테스트")
	class TransactionRollbackTest {

		@Test
		@DisplayName("좌석 할당 중 예외 발생 시 예외가 전파된다")
		void selectSeatAndCreateTicket_TicketCreationFail_ThrowsException() {
			// given
			given(executor.selectSeat(eventId, seatId, userId))
				.willThrow(new RuntimeException("좌석 예약 실패"));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("좌석 예약 실패");

			then(executor).should().selectSeat(eventId, seatId, userId);
		}
	}
}
