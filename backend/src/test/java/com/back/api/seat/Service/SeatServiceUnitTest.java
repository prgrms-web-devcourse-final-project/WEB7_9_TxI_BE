package com.back.api.seat.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.api.seat.service.SeatService;
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

			// .willReturn(true)로 실제 구현없이 항상 true 반환
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(eventRepository.existsById(eventId)).willReturn(true);
			given(seatRepository.findSortedSeatListByEventId(eventId)).willReturn(expectedSeats);

			// when
			List<Seat> result = seatService.getSeatsByEvent(eventId, userId);

			// then
			assertThat(result).hasSize(3);
			assertThat(result).isEqualTo(expectedSeats);
			// Mock 객체가 호출되었는지 여부를 검증
			// Mockito는 내부적으로 모든 Mock 호출을 기록(tracking)하고 있음
			// 반환값과는 무관하게 then.should().메소드()에서 메소드의 실행여부만 확인
			then(queueEntryRedisRepository).should().isInEnteredQueue(eventId, userId); // 메소드 호출 검증
			then(eventRepository).should().existsById(eventId);
			then(seatRepository).should().findSortedSeatListByEventId(eventId);
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자는 좌석 조회에 실패")
		void getSeatsByEvent_NotInQueue_ThrowsException() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.getSeatsByEvent(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_IN_QUEUE);

			then(eventRepository).should(never()).existsById(any());
			then(seatRepository).should(never()).findSortedSeatListByEventId(any());
		}

		@Test
		@DisplayName("존재하지 않는 이벤트의 좌석 조회에 실패")
		void getSeatsByEvent_EventNotFound_ThrowsException() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(eventRepository.existsById(eventId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.getSeatsByEvent(eventId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_FOUND_EVENT);

			then(seatRepository).should(never()).findSortedSeatListByEventId(any());
		}
	}

	@Nested
	@DisplayName("selectSeat 테스트")
	class SelectSeatTest {

		@Test
		@DisplayName("정상적으로 좌석을 선택한다")
		void selectSeat_Success() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.selectSeat(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
			then(queueEntryRedisRepository).should().isInEnteredQueue(eventId, userId);
			then(seatRepository).should().findByEventIdAndId(eventId, seatId);
			then(seatRepository).should().save(testSeat);
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("큐에 입장하지 않은 사용자는 좌석 선택에 실패한다")
		void selectSeat_NotInQueue_ThrowsException() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_IN_QUEUE);

			then(seatRepository).should(never()).findByEventIdAndId(any(), any());
			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("존재하지 않는 좌석 선택에 실패한다")
		void selectSeat_SeatNotFound_ThrowsException() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_FOUND_SEAT);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("이미 예약된 좌석 선택에 실패한다")
		void selectSeat_AlreadyReserved_ThrowsException() {
			// given
			testSeat.markAsReserved(); // 좌석을 미리 예약 상태로 변경

			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_ALREADY_RESERVED);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("이미 판매된 좌석 선택에 실패한다")
		void selectSeat_AlreadySold_ThrowsException() {
			// given
			testSeat.markAsReserved();
			testSeat.markAsSold(); // 좌석을 판매 상태로 변경

			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_ALREADY_SOLD);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("낙관적 락 충돌 발생 시 SEAT_CONCURRENCY_FAILURE 예외를 던진다")
		void selectSeat_OptimisticLockingFailure_ThrowsException() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class)))
				.willThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}
	}

	@Nested
	@DisplayName("confirmPurchase 테스트")
	class ConfirmPurchaseTest {

		@Test
		@DisplayName("정상적으로 구매를 확정한다")
		void confirmPurchase_Success() {
			// given
			testSeat.markAsReserved(); // 먼저 예약 상태로 변경

			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.confirmPurchase(eventId, seatId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
			then(seatRepository).should().findByEventIdAndId(eventId, seatId);
			then(seatRepository).should().save(testSeat);
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("존재하지 않는 좌석의 구매 확정에 실패한다")
		void confirmPurchase_SeatNotFound_ThrowsException() {
			// given
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> seatService.confirmPurchase(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.NOT_FOUND_SEAT);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("예약되지 않은 좌석의 구매 확정에 실패한다")
		void confirmPurchase_NotReserved_ThrowsException() {
			// given - testSeat은 기본적으로 AVAILABLE 상태
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// when & then
			assertThatThrownBy(() -> seatService.confirmPurchase(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_STATUS_TRANSITION);

			then(seatRepository).should(never()).save(any());
			then(eventPublisher).should(never()).publishEvent(any());
		}

		@Test
		@DisplayName("낙관적 락 충돌 발생 시 SEAT_CONCURRENCY_FAILURE 예외를 던진다")
		void confirmPurchase_OptimisticLockingFailure_ThrowsException() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class)))
				.willThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));

			// when & then
			assertThatThrownBy(() -> seatService.confirmPurchase(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}
	}

	@Nested
	@DisplayName("동시성 테스트")
	class ConcurrencyTest {

		@Test
		@DisplayName("여러 사용자가 동시에 같은 좌석을 선택할 때 하나만 성공한다")
		void selectSeat_Concurrent_OnlyOneSuccess() throws InterruptedException {
			// given
			int threadCount = 10;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch latch = new CountDownLatch(threadCount);
			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger failureCount = new AtomicInteger(0);

			// 첫 번째 호출은 성공, 이후 호출은 OptimisticLockingFailure 또는 알려진 에러를 던지도록 설정
			given(queueEntryRedisRepository.isInEnteredQueue(eq(eventId), anyLong())).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// 첫 번째는 성공, 나머지는 낙관적 락 예외
			given(seatRepository.save(any(Seat.class)))
				.willReturn(testSeat)
				.willThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));

			// when
			for (int i = 0; i < threadCount; i++) {
				final long currentUserId = userId + i;
				executorService.submit(() -> {
					try {
						seatService.selectSeat(eventId, seatId, currentUserId);
						successCount.incrementAndGet();
					} catch (ErrorException e) {
						if (e.getErrorCode() == SeatErrorCode.SEAT_CONCURRENCY_FAILURE
							|| e.getErrorCode() == SeatErrorCode.SEAT_ALREADY_RESERVED) {
							failureCount.incrementAndGet();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await();
			executorService.shutdown();

			// then
			assertThat(successCount.get()).isEqualTo(1);
			assertThat(failureCount.get()).isEqualTo(threadCount - 1);
			assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
		}

		@Test
		@DisplayName("여러 사용자가 동시에 구매 확정 시 하나만 성공한다")
		void confirmPurchase_Concurrent_OnlyOneSuccess() throws InterruptedException {
			// given
			testSeat.markAsReserved(); // 좌석을 예약 상태로 변경

			int threadCount = 10;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch latch = new CountDownLatch(threadCount);
			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger failureCount = new AtomicInteger(0);

			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// 첫 번째는 성공, 나머지는 낙관적 락 예외
			given(seatRepository.save(any(Seat.class)))
				.willReturn(testSeat)
				.willThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));

			// when
			for (int i = 0; i < threadCount; i++) {
				final long currentUserId = userId + i;
				executorService.submit(() -> {
					try {
						seatService.confirmPurchase(eventId, seatId, currentUserId);
						successCount.incrementAndGet();
					} catch (ErrorException e) {
						if (e.getErrorCode() == SeatErrorCode.SEAT_CONCURRENCY_FAILURE
							|| e.getErrorCode() == SeatErrorCode.SEAT_STATUS_TRANSITION) {
							failureCount.incrementAndGet();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await();
			executorService.shutdown();

			// then
			assertThat(successCount.get()).isEqualTo(1);
			assertThat(failureCount.get()).isEqualTo(threadCount - 1);
			assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
		}

		@Test
		@DisplayName("서로 다른 좌석에 대한 동시 선택은 모두 성공한다")
		void selectDifferentSeats_Concurrent_AllSuccess() throws InterruptedException {
			// given
			int threadCount = 5;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch latch = new CountDownLatch(threadCount);
			AtomicInteger successCount = new AtomicInteger(0);
			List<Seat> seats = new ArrayList<>();

			// 서로 다른 좌석들 생성
			for (int i = 0; i < threadCount; i++) {
				Seat seat = Seat.createSeat(testEvent, "A" + (i + 1), SeatGrade.VIP, 150000);
				seats.add(seat);
			}

			given(queueEntryRedisRepository.isInEnteredQueue(eq(eventId), anyLong())).willReturn(true);

			// Mock 설정을 먼저 모두 완료
			for (int i = 0; i < threadCount; i++) {
				final long currentSeatId = seatId + i;
				given(seatRepository.findByEventIdAndId(eventId, currentSeatId))
					.willReturn(Optional.of(seats.get(i)));
				given(seatRepository.save(seats.get(i))).willReturn(seats.get(i));
			}

			// when - 스레드 실행
			for (int i = 0; i < threadCount; i++) {
				final long currentSeatId = seatId + i;
				final long currentUserId = userId + i;

				executorService.submit(() -> {
					try {
						seatService.selectSeat(eventId, currentSeatId, currentUserId);
						successCount.incrementAndGet();
					} catch (Exception e) {
						// 예외 발생하면 안됨
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await();
			executorService.shutdown();

			// then
			assertThat(successCount.get()).isEqualTo(threadCount);
		}
	}

	@Nested
	@DisplayName("좌석 상태 변경 검증 테스트")
	class SeatStatusTransitionTest {

		@Test
		@DisplayName("AVAILABLE -> RESERVED 상태 변경이 성공한다")
		void seatStatus_AvailableToReserved_Success() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.selectSeat(eventId, seatId, userId);

			// then
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		}

		@Test
		@DisplayName("RESERVED -> SOLD 상태 변경이 성공한다")
		void seatStatus_ReservedToSold_Success() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			Seat result = seatService.confirmPurchase(eventId, seatId, userId);

			// then
			assertThat(result.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
		}

		@Test
		@DisplayName("AVAILABLE -> SOLD 상태 변경은 실패한다")
		void seatStatus_AvailableToSold_Fails() {
			// given - testSeat은 기본적으로 AVAILABLE 상태
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));

			// when & then
			assertThatThrownBy(() -> seatService.confirmPurchase(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_STATUS_TRANSITION);
		}
	}

	@Nested
	@DisplayName("이벤트 발행 검증 테스트")
	class EventPublishTest {

		@Test
		@DisplayName("좌석 선택 시 SeatStatusMessage 이벤트가 발행된다")
		void selectSeat_PublishesEvent() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(true);
			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.selectSeat(eventId, seatId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("구매 확정 시 SeatStatusMessage 이벤트가 발행된다")
		void confirmPurchase_PublishesEvent() {
			// given
			testSeat.markAsReserved();

			given(seatRepository.findByEventIdAndId(eventId, seatId)).willReturn(Optional.of(testSeat));
			given(seatRepository.save(any(Seat.class))).willReturn(testSeat);

			// when
			seatService.confirmPurchase(eventId, seatId, userId);

			// then
			then(eventPublisher).should().publishEvent(any(SeatStatusMessage.class));
		}

		@Test
		@DisplayName("좌석 선택 실패 시 이벤트가 발행되지 않는다")
		void selectSeat_Failure_DoesNotPublishEvent() {
			// given
			given(queueEntryRedisRepository.isInEnteredQueue(eventId, userId)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> seatService.selectSeat(eventId, seatId, userId))
				.isInstanceOf(ErrorException.class);

			then(eventPublisher).should(never()).publishEvent(any());
		}
	}
}
