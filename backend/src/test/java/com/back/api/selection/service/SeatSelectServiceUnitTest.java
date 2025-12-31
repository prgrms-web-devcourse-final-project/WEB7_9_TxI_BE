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
import com.back.domain.store.entity.Store;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
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

			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(ticketService.getOrCreateDraft(eventId, userId)).willReturn(draftTicket);
			given(seatService.reserveSeat(eventId, seatId, userId)).willReturn(testSeat);

			// when
			Ticket result = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
			assertThat(result.getOwner()).isEqualTo(testUser);
			assertThat(result.getEvent()).isEqualTo(testEvent);
			assertThat(result.getSeat()).isEqualTo(testSeat);

			then(queueEntryReadService).should().isUserEntered(eventId, userId);
			then(ticketService).should().getOrCreateDraft(eventId, userId);
			then(seatService).should().reserveSeat(eventId, seatId, userId);
			then(seatService).should(never()).markSeatAsAvailable(any());  // 기존 좌석 없음
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

			then(ticketService).should(never()).getOrCreateDraft(any(), any());
			then(seatService).should(never()).reserveSeat(any(), any(), any());
		}

		@Test
		@DisplayName("좌석 예약 실패 시 예외가 발생한다")
		void selectSeatAndCreateTicket_ReserveFail_DoesNotCreateTicket() {
			// given
			Ticket draftTicket = Ticket.builder()
				.owner(testUser)
				.event(testEvent)
				.seat(null)
				.ticketStatus(TicketStatus.DRAFT)
				.build();

			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(ticketService.getOrCreateDraft(eventId, userId)).willReturn(draftTicket);
			given(seatService.reserveSeat(eventId, seatId, userId))
				.willThrow(new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_ALREADY_RESERVED);

			then(ticketService).should().getOrCreateDraft(eventId, userId);
		}
	}

	@Nested
	@DisplayName("좌석 상태 검증 테스트")
	class SeatStatusValidationTest {

		@Test
		@DisplayName("좌석 선택 성공 시 좌석이 RESERVED 상태가 된다")
		void selectSeat_SeatBecomesReserved() {
			// given
			Ticket draftTicket = Ticket.builder()
				.owner(testUser)
				.event(testEvent)
				.seat(null)
				.ticketStatus(TicketStatus.DRAFT)
				.build();

			testSeat.markAsReserved(); // 예약 상태로 변경

			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(ticketService.getOrCreateDraft(eventId, userId)).willReturn(draftTicket);
			given(seatService.reserveSeat(eventId, seatId, userId)).willReturn(testSeat);

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
		@DisplayName("좌석 할당 중 예외 발생 시 예외가 전파된다")
		void selectSeatAndCreateTicket_TicketCreationFail_ThrowsException() {
			// given
			Ticket draftTicket = Ticket.builder()
				.owner(testUser)
				.event(testEvent)
				.seat(null)
				.ticketStatus(TicketStatus.DRAFT)
				.build();

			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(ticketService.getOrCreateDraft(eventId, userId)).willReturn(draftTicket);
			given(seatService.reserveSeat(eventId, seatId, userId))
				.willThrow(new RuntimeException("좌석 예약 실패"));

			// when & then
			assertThatThrownBy(() -> seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("좌석 예약 실패");

			// Draft Ticket은 조회되었지만, 좌석 예약 실패로 트랜잭션 롤백
			then(ticketService).should().getOrCreateDraft(eventId, userId);
			then(seatService).should().reserveSeat(eventId, seatId, userId);
		}
	}
}
