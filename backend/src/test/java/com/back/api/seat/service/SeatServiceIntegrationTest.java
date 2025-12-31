package com.back.api.seat.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import com.back.api.queue.service.QueueEntryProcessService;
import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.support.factory.EventFactory;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.UserHelper;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class SeatServiceIntegrationTest {

	@Autowired
	private SeatService seatService;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Autowired
	private QueueEntryProcessService queueEntryProcessService;

	@Autowired
	private SeatHelper seatHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private QueueEntryRepository queueEntryRepository;

	@Autowired
	private UserHelper userHelper;

	private Event event;

	@BeforeEach
	void setUp() {
		Store store = storeHelper.createStore();
		event = EventFactory.fakeEvent(store, "테스트 콘서트");
		eventRepository.save(event);

		// Redis 초기화
		queueEntryRedisRepository.clearAll(event.getId());
	}

	@Test
	@DisplayName("이벤트의 좌석 목록 조회 성공")
	void getSeatsByEvent_Success() {

		seatHelper.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatHelper.createSeat(event, "A2", SeatGrade.S, 100000);
		seatHelper.createSeat(event, "A3", SeatGrade.R, 50000);

		// User 생성
		User user = userHelper.createUser(UserRole.NORMAL, null).user();
		Long eventId = event.getId();
		Long userId = user.getId();

		// QueueEntry 생성 (WAITING 상태)
		QueueEntry queueEntry = new QueueEntry(user, event, 1);
		queueEntryRepository.save(queueEntry);

		// WAITING -> ENTERED 상태로 변경
		queueEntryProcessService.processEntry(eventId, userId);

		List<Seat> seats = seatService.getSeatsByEvent(eventId, userId);

		assertThat(seats).hasSize(3);
		assertThat(seats).extracting(Seat::getSeatCode).containsExactly("A3", "A2", "A1");
	}

	@Test
	@DisplayName("존재하지 않는 이벤트의 좌석 조회 시 예외 발생")
	void getSeatsByEvent_EventNotFound_Fail() {

		Long notExistEventId = 999L;
		Long userId = 1L;

		assertThatThrownBy(() ->
			seatService.getSeatsByEvent(notExistEventId, userId)
		).isInstanceOf(ErrorException.class);
	}

	@Test
	@DisplayName("좌석 예약 성공 - AVAILABLE → RESERVED 상태 전이")
	void reserveSeat_StatusTransition_Success() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = 1L;

		Seat reserved = seatService.reserveSeat(eventId, seatId, userId);

		assertThat(reserved.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		assertThat(reserved.getVersion()).isEqualTo(1); // version 증가 확인
	}

	@Test
	@DisplayName("동시에 동일 좌석 예약 시 하나만 성공하고 나머지는 OptimisticLock 발생")
	void reserveSeat_OptimisticLock_OnlyOneSuccess() throws InterruptedException {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();

		int threadCount = 10;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			final long uid = i + 1;

			executor.submit(() -> {
				try {
					seatService.reserveSeat(eventId, seatId, uid);
					success.incrementAndGet();
				} catch (ErrorException | ObjectOptimisticLockingFailureException e) {
					failure.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		assertThat(success.get()).isEqualTo(1);
		assertThat(failure.get()).isEqualTo(threadCount - 1);
	}

	@Test
	@DisplayName("좌석 판매 완료 - RESERVED → SOLD 상태 전이")
	void markSeatAsSold_StatusTransition_Success() {

		// AVAILABLE 상태로 먼저 저장 (version = 0)
		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = 1L;

		// RESERVED로 변경 (version = 1)
		Seat reserved = seatService.reserveSeat(eventId, seatId, userId);

		// SOLD로 변경 (version = 2)
		seatService.markSeatAsSold(reserved);

		Seat updatedSeat = seatRepository.findById(seatId).orElseThrow();
		assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
		assertThat(updatedSeat.getVersion()).isEqualTo(2);  // AVAILABLE(0) → RESERVED(1) → SOLD(2)
	}

	@Test
	@DisplayName("좌석 복구 - RESERVED → AVAILABLE 상태 전이")
	void markSeatAsAvailable_StatusTransition_Success() {

		// AVAILABLE 상태로 먼저 저장 (version = 0)
		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = 1L;

		// RESERVED로 변경 (version = 1)
		Seat reserved = seatService.reserveSeat(eventId, seatId, userId);

		// AVAILABLE로 복구 (version = 2)
		seatService.markSeatAsAvailable(reserved);

		Seat updatedSeat = seatRepository.findById(seatId).orElseThrow();
		assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
		assertThat(updatedSeat.getVersion()).isEqualTo(2);  // AVAILABLE(0) → RESERVED(1) → AVAILABLE(2)
	}

	@Test
	@DisplayName("AVAILABLE 상태에서 SOLD로 변경 시 실패")
	void markSeatAsSold_InvalidTransition_Fail() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		assertThatThrownBy(() ->
			seatService.markSeatAsSold(seat)
		).isInstanceOf(ErrorException.class)
			.hasFieldOrPropertyWithValue("errorCode", SeatErrorCode.SEAT_STATUS_TRANSITION);
	}
}
