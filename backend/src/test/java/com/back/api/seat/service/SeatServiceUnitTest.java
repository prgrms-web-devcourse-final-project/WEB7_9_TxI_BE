package com.back.api.seat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService 단위 테스트")
class SeatServiceUnitTest {

	@InjectMocks
	private SeatService seatService;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private QueueEntryReadService queueEntryReadService;

	@Mock
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Mock
	private EventPublisher eventPublisher;

	private Event testEvent;
	private Seat testSeat;
	private Long eventId;
	private Long seatId;
	private Long userId;

	@BeforeEach
	void setUp() {
		eventId = 1L;
		seatId = 1L;
		userId = 100L;

		testEvent = Event.builder()
			.title("테스트 콘서트")
			.category(EventCategory.CONCERT)
			.description("테스트 설명")
			.place("테스트 장소")
			.imageUrl("https://test.com/image.jpg")
			.minPrice(50000)
			.maxPrice(150000)
			.preOpenAt(LocalDateTime.now().plusDays(1))
			.preCloseAt(LocalDateTime.now().plusDays(2))
			.ticketOpenAt(LocalDateTime.now().plusDays(3))
			.ticketCloseAt(LocalDateTime.now().plusDays(4))
			.eventDate(LocalDateTime.now().plusDays(10))
			.maxTicketAmount(100)
			.status(EventStatus.READY)
			.build();

		testSeat = Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000);
	}

	@Nested
	@DisplayName("getSeatsByEvent 테스트")
	class GetSeatsByEventTest {

		@Test
		@DisplayName("정상적으로 좌석 목록 조회")
		void getSeatsByEvent_Success() {
			// given
			List<Seat> expectedSeats = List.of(
				Seat.createSeat(testEvent, "A1", SeatGrade.VIP, 150000),
				Seat.createSeat(testEvent, "A2", SeatGrade.VIP, 150000),
				Seat.createSeat(testEvent, "B1", SeatGrade.R, 100000)
			);

			given(eventRepository.existsById(eventId)).willReturn(true);
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(true);
			given(seatRepository.findSortedSeatListByEventId(eventId)).willReturn(expectedSeats);

			// when
			List<Seat> result = seatService.getSeatsByEvent(eventId, userId);

			// then
			assertThat(result).hasSize(3);
			assertThat(result).isEqualTo(expectedSeats);
			then(eventRepository).should().existsById(eventId);
			then(queueEntryReadService).should().isUserEntered(eventId, userId);
			then(seatRepository).should().findSortedSeatListByEventId(eventId);
		}

		@Test
		@DisplayName("존재하지 않는 이벤트의 좌석 조회에 실패")
		void getSeatsByEvent_EventNotFound_ThrowsException() {
			// given
			given(eventRepository.existsById(eventId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.getSeatsByEvent(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_FOUND_EVENT);

			then(queueEntryReadService).should(never()).isUserEntered(any(), any());
			then(seatRepository).should(never()).findSortedSeatListByEventId(any());
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자의 좌석 조회에 실패")
		void getSeatsByEvent_NotInQueue_ThrowsException() {
			// given
			given(eventRepository.existsById(eventId)).willReturn(true);
			given(queueEntryReadService.isUserEntered(eventId, userId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.getSeatsByEvent(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_IN_QUEUE);

			then(eventRepository).should().existsById(eventId);
			then(queueEntryReadService).should().isUserEntered(eventId, userId);
			then(seatRepository).should(never()).findSortedSeatListByEventId(any());
		}
	}

	@Nested
	@DisplayName("reserveSeat 테스트")
	class ReserveSeatTest {

		@Test
		@DisplayName("정상적으로 좌석을 예약한다")
		void reserveSeat_Success() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.reserveSeat(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
			then(seatRepository).should().findByEventIdAndId(eventId, seatId);
			then(seatRepository).should().save(testSeat);
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("존재하지 않는 좌석 예약에 실패한다")
		void reserveSeat_SeatNotFound_ThrowsException() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> seatService.reserveSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_FOUND_SEAT);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("낙관적 락 충돌 발생 시 SEAT_CONCURRENCY_FAILURE 예외를 던진다")
		void reserveSeat_OptimisticLockingFailure_ThrowsException() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class)))
				.willThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));

			// when & then
			assertThatThrownBy(() -> seatService.reserveSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}
	}

	@Nested
	@DisplayName("markSeatAsSold 테스트")
	class MarkSeatAsSoldTest {

		@Test
		@DisplayName("정상적으로 좌석을 판매 완료 상태로 변경한다")
		void markSeatAsSold_Success() {
			// given
			testSeat.markAsReserved(); // 먼저 예약 상태로 변경

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsSold(testSeat);

			// then
			assertThat(testSeat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
			then(seatRepository).should().save(testSeat);
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("AVAILABLE 상태에서 SOLD로 변경 시 실패한다")
		void markSeatAsSold_FromAvailable_ThrowsException() {
			// given - testSeat은 기본적으로 AVAILABLE 상태

			// when & then
			assertThatThrownBy(() -> seatService.markSeatAsSold(testSeat))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_STATUS_TRANSITION);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}
	}

	@Nested
	@DisplayName("markSeatAsAvailable 테스트")
	class MarkSeatAsAvailableTest {

		@Test
		@DisplayName("정상적으로 좌석을 사용 가능 상태로 복구한다")
		void markSeatAsAvailable_Success() {
			// given
			testSeat.markAsReserved(); // 먼저 예약 상태로 변경

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsAvailable(testSeat);

			// then
			assertThat(testSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
			then(seatRepository).should().save(testSeat);
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}
	}


	@Nested
	@DisplayName("좌석 상태 변경 검증 테스트")
	class SeatStatusTransitionTest {

		@Test
		@DisplayName("AVAILABLE -> RESERVED 상태 변경이 성공한다")
		void seatStatus_AvailableToReserved_Success() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.reserveSeat(eventId, seatId, userId);

			// then
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		}

		@Test
		@DisplayName("RESERVED -> SOLD 상태 변경이 성공한다")
		void seatStatus_ReservedToSold_Success() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsSold(testSeat);

			// then
			assertThat(testSeat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
		}

		@Test
		@DisplayName("RESERVED -> AVAILABLE 상태 변경이 성공한다")
		void seatStatus_ReservedToAvailable_Success() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsAvailable(testSeat);

			// then
			assertThat(testSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
		}
	}

	@Nested
	@DisplayName("이벤트 발행 검증 테스트")
	class EventPublishTest {

		@Test
		@DisplayName("좌석 예약 시 SeatStatusMessage 이벤트가 발행된다")
		void reserveSeat_PublishesEvent() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.reserveSeat(eventId, seatId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("좌석 판매 완료 시 SeatStatusMessage 이벤트가 발행된다")
		void markSeatAsSold_PublishesEvent() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsSold(testSeat);

			// then
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("좌석 복구 시 SeatStatusMessage 이벤트가 발행된다")
		void markSeatAsAvailable_PublishesEvent() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.markSeatAsAvailable(testSeat);

			// then
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("좌석 예약 실패 시 이벤트가 발행되지 않는다")
		void reserveSeat_Failure_DoesNotPublishEvent() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> seatService.reserveSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class);

			then(eventPublisher).should(never()).publishEvent(any());
		}
	}
}
