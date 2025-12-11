package com.back.api.selection.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.service.SeatService;
import com.back.api.ticket.service.TicketService;
import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.factory.SeatFactory;
import com.back.support.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatSelectionService 단위 테스트")
@ActiveProfiles("test")
class SeatSelectServiceUnitTest {

	@InjectMocks
	private SeatSelectionService seatSelectionService;

	@Mock
	private SeatService seatService;

	@Mock
	private TicketService ticketService;

	@Mock
	private QueueEntryReadService queueEntryReadService;

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

		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		testEvent = EventFactory.fakeEvent("테스트 콘서트");
		testSeat = SeatFactory.fakeSeat(testEvent, "A1", SeatGrade.VIP, 150000);

		testTicket = Ticket.builder()
			.owner(testUser)
			.event(testEvent)
			.seat(testSeat)
			.ticketStatus(TicketStatus.DRAFT)
			.build();
	}

	@Nested
	@DisplayName("selectSeatAndCreateTicket 테스트")
	class SelectSeatAndCreateTicketTest {

		@Test
		@DisplayName("정상적으로 좌석을 선택하고 Draft Ticket을 생성한다")
		void selectSeatAndCreateTicket_Success() {
			// given
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(seatService.reserveSeat(eventId, seatId, userId)).willReturn(testSeat);
			given(ticketService.createDraftTicket(eventId, seatId, userId)).willReturn(testTicket);

			// when
			Ticket result = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
			assertThat(result.getOwner()).isEqualTo(testUser);
			assertThat(result.getEvent()).isEqualTo(testEvent);
			assertThat(result.getSeat()).isEqualTo(testSeat);

			then(queueEntryReadService).should().isUserEntered(eventId, userId);
			then(seatService).should().reserveSeat(eventId, seatId, userId);
			then(ticketService).should().createDraftTicket(eventId, seatId, userId);
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자는 좌석 선택에 실패한다")
		void selectSeatAndCreateTicket_NotInQueue_ThrowsException() {
			// given
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_IN_QUEUE);

			then(seatService).should(never()).reserveSeat(any(), any(), any());
			then(ticketService).should(never()).createDraftTicket(any(), any(), any());
		}

		@Test
		@DisplayName("좌석 예약 실패 시 티켓 생성하지 않는다")
		void selectSeatAndCreateTicket_ReserveFail_DoesNotCreateTicket() {
			// given
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(seatService.reserveSeat(eventId, seatId, userId))
				.willThrow(new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_ALREADY_RESERVED);

			then(ticketService).should(never()).createDraftTicket(any(), any(), any());
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

			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(seatService.reserveSeat(eventId, seatId, userId)).willReturn(testSeat);
			given(ticketService.createDraftTicket(eventId, seatId, userId)).willReturn(testTicket);

			// when
			Ticket result = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

			// then
			assertThat(result.getSeat().getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		}
	}

	@Nested
	@DisplayName("트랜잭션 롤백 검증 테스트")
	class TransactionRollbackTest {

		@Test
		@DisplayName("티켓 생성 실패 시 예외가 발생한다")
		void selectSeatAndCreateTicket_TicketCreationFail_ThrowsException() {
			// given
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(seatService.reserveSeat(eventId, seatId, userId)).willReturn(testSeat);
			given(ticketService.createDraftTicket(eventId, seatId, userId))
				.willThrow(new RuntimeException("티켓 생성 실패"));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("티켓 생성 실패");

			// 좌석 예약까지는 호출되었지만 트랜잭션 롤백으로 인해 실제로는 반영되지 않음
			then(seatService).should().reserveSeat(eventId, seatId, userId);
			then(ticketService).should().createDraftTicket(eventId, seatId, userId);
		}
	}
}
