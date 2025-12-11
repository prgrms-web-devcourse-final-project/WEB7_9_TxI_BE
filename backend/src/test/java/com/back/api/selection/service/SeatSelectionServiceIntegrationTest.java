package com.back.api.selection.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.exception.ErrorException;
import com.back.support.data.TestUser;
import com.back.support.factory.EventFactory;
import com.back.support.factory.UserFactory;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class SeatSelectionServiceIntegrationTest {

	@Autowired
	private SeatSelectionService seatSelectionService;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Event event;
	private User user;

	@BeforeEach
	void setUp() {
		event = EventFactory.fakeEvent("테스트 콘서트");
		eventRepository.save(event);

		long uniqueId = System.nanoTime() % 100000000;
		user = User.builder()
			.email("user" + uniqueId + "@ex.com")
			.nickname("user" + uniqueId)
			.password("password123")
			.role(UserRole.NORMAL)
			.activeStatus(UserActiveStatus.ACTIVE)
			.build();
		userRepository.save(user);

		// Redis 초기화
		queueEntryRedisRepository.clearAll(event.getId());
	}

	@Test
	@DisplayName("좌석 선택 시 AVAILABLE → RESERVED 상태 전이 및 Draft Ticket 생성 성공")
	void selectSeatAndCreateTicket_Success() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = user.getId();

		// 큐 입장 처리
		queueEntryRedisRepository.moveToEnteredQueue(eventId, userId);

		Ticket draftTicket = seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId);

		// 티켓 검증
		assertThat(draftTicket).isNotNull();
		assertThat(draftTicket.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
		assertThat(draftTicket.getOwner().getId()).isEqualTo(userId);
		assertThat(draftTicket.getEvent().getId()).isEqualTo(eventId);
		assertThat(draftTicket.getSeat().getId()).isEqualTo(seatId);

		// 좌석 상태 검증
		Seat updatedSeat = seatRepository.findById(seatId).orElseThrow();
		assertThat(updatedSeat.getSeatStatus()).isEqualTo(SeatStatus.RESERVED);
		assertThat(updatedSeat.getVersion()).isEqualTo(1); // version 증가 확인
	}

	@Test
	@DisplayName("동시에 동일 좌석 선택 시 하나만 성공하고 나머지는 실패")
	void selectSeatAndCreateTicket_OptimisticLock_OnlyOneSuccess() throws InterruptedException {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();

		int threadCount = 10;

		// 모든 사용자를 생성하고 큐에 입장시킴
		List<Long> enteredUserIds = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder);
			userRepository.save(testUser.user());

			queueEntryRedisRepository.moveToEnteredQueue(eventId, testUser.user().getId());
			enteredUserIds.add(testUser.user().getId());
		}

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			final long uid = enteredUserIds.get(i);

			executor.submit(() -> {
				try {
					seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, uid);
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
	@DisplayName("큐에 입장하지 않은 사용자는 좌석 선택 불가")
	void selectSeatAndCreateTicket_NotInQueue_Fail() {

		Seat seat = Seat.createSeat(event, "A1", SeatGrade.VIP, 150000);
		seatRepository.save(seat);

		Long eventId = event.getId();
		Long seatId = seat.getId();
		Long userId = user.getId();

		// 큐에 입장하지 않음

		assertThatThrownBy(() ->
			seatSelectionService.selectSeatAndCreateTicket(eventId, seatId, userId)
		).isInstanceOf(ErrorException.class);
	}
}
