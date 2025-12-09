package com.back.api.seat.Service;

import static org.assertj.core.api.Assertions.*;

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

import com.back.api.seat.service.SeatService;
import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.exception.ErrorException;

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
	private com.back.domain.queue.repository.QueueEntryRedisRepository queueEntryRedisRepository;

	private Event event;

	@BeforeEach
	void setUp() {
		event = Event.builder()
			.title("Concert")
			.category(EventCategory.CONCERT)
			.description("Desc")
			.place("Seoul")
			.imageUrl("image")
			.minPrice(50000)
			.maxPrice(150000)
			.preOpenAt(java.time.LocalDateTime.now())
			.preCloseAt(java.time.LocalDateTime.now().plusDays(1))
			.ticketOpenAt(java.time.LocalDateTime.now().plusDays(2))
			.ticketCloseAt(java.time.LocalDateTime.now().plusDays(3))
			.maxTicketAmount(100)
			.status(EventStatus.OPEN)
			.build();

		eventRepository.save(event);

		// Redis 큐 데이터 초기화
		queueEntryRedisRepository.clearAll(event.getId());
	}

	@Test
	@DisplayName("AVAILABLE → RESERVED 상태 전이 성공하고 version 증가 확인")
	void selectSeat_StatusTransition_Success() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = 1L;

		// 큐 입장 처리
		queueEntryRedisRepository.moveToEnteredQueue(eventId, userId);

		Seat selected = seatService.selectSeat(eventId, seatId, userId);

		assertThat(selected.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		assertThat(selected.getVersion()).isEqualTo(1); // version 증가 확인
	}

	@Test
	@DisplayName("동시에 동일 좌석 선택 시 하나만 성공하고 나머지는 OptimisticLock 발생")
	void selectSeat_OptimisticLock_OnlyOneSuccess() throws InterruptedException {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();

		int threadCount = 10;

		// 모든 사용자를 큐에 입장시킴
		for (int i = 0; i < threadCount; i++) {
			queueEntryRedisRepository.moveToEnteredQueue(eventId, (long)(i + 1));
		}

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			final long uid = i + 1;

			executor.submit(() -> {
				try {
					seatService.selectSeat(eventId, seatId, uid);
					success.incrementAndGet();
				} catch (ErrorException | ObjectOptimisticLockingFailureException e) {
					failure.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		assertThat(success.get()).isEqualTo(1);
		assertThat(failure.get()).isEqualTo(threadCount - 1);
	}

	@Test
	@DisplayName("RESERVED → SOLD 상태 전이 성공")
	void confirmPurchase_StatusTransition_Success() {

		// AVAILABLE 상태로 먼저 저장 (version = 0)
		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		// RESERVED로 변경 후 저장 (version = 1)
		seat.markAsReserved();
		seatRepository.save(seat);

		// SOLD로 변경 (version = 2)
		Seat sold = seatService.confirmPurchase(event.getId(), seat.getId(), 1L);

		assertThat(sold.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
		assertThat(sold.getVersion()).isEqualTo(2);  // AVAILABLE(0) → RESERVED(1) → SOLD(2)
	}

	@Test
	@DisplayName("AVAILABLE 상태에서 confirmPurchase 하면 실패한다")
	void confirmPurchase_InvalidTransition() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		assertThatThrownBy(() ->
			seatService.confirmPurchase(event.getId(), seat.getId(), 1L)
		).isInstanceOf(ErrorException.class);
	}

}
