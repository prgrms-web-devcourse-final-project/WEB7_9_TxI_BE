package com.back.api.ticket.scheduler;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
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
import com.back.support.helper.EventHelper;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("임시 비활성화 프론트 개발")
@DisplayName("DraftTicketExpirationScheduler 통합 테스트")
class DraftTicketExpirationSchedulerTest {

	@Autowired
	private DraftTicketExpirationScheduler scheduler;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private SeatHelper seatHelper;

	@Autowired
	private TicketHelper ticketHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private EntityManager em;

	private User user;
	private Event event;
	private Seat seat;

	@BeforeEach
	void setUp() {
		Store store = storeHelper.createStore();
		// Helper 메서드들이 각자 @Transactional로 커밋
		user = userHelper.createUser(UserRole.NORMAL, null).user();
		event = eventHelper.createEvent(store);
		seat = seatHelper.createSeat(event, "A1", SeatGrade.VIP);
	}

	@Test
	@Transactional
	@DisplayName("좌석이 할당된 만료된 Draft 티켓을 정상적으로 처리한다")
	void expireDraftTicket_withSeat_success() throws Exception {
		// given: 5분 이상 경과한 Draft 티켓 생성 (좌석 할당됨)
		Ticket ticket = ticketHelper.createDraftTicket(user, seat, event);
		setCreatedAt(ticket, LocalDateTime.now().minusMinutes(10));

		// 좌석을 RESERVED 상태로 변경
		seat.markAsReserved();
		seatRepository.save(seat);

		// 트랜잭션 커밋하여 스케줄러가 데이터를 볼 수 있도록 함
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// when: 스케줄러 실행 (Lock 없이, 별도 트랜잭션)
		scheduler.expireDraftTicketsInternal();

		// then: 새 트랜잭션 시작하여 결과 확인
		TestTransaction.start();
		Ticket expiredTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(expiredTicket.getTicketStatus()).isEqualTo(TicketStatus.FAILED);

		// then: 좌석이 AVAILABLE 상태로 변경됨
		Seat releasedSeat = seatRepository.findById(seat.getId()).orElseThrow();
		assertThat(releasedSeat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
	}

	@Test
	@Transactional
	@DisplayName("좌석이 없는 만료된 Draft 티켓도 정상적으로 처리한다")
	void expireDraftTicket_withoutSeat_success() throws Exception {
		// given: 5분 이상 경과한 Draft 티켓 생성 (좌석 없음)
		Ticket ticket = Ticket.builder()
			.owner(user)
			.event(event)
			.seat(null)  // 좌석 없음
			.ticketStatus(TicketStatus.DRAFT)
			.build();
		ticketRepository.save(ticket);
		setCreatedAt(ticket, LocalDateTime.now().minusMinutes(10));

		// 트랜잭션 커밋하여 스케줄러가 데이터를 볼 수 있도록 함
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// when: 스케줄러 실행 (Lock 없이, 별도 트랜잭션)
		scheduler.expireDraftTicketsInternal();

		// then: 새 트랜잭션 시작하여 결과 확인
		TestTransaction.start();
		Ticket expiredTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(expiredTicket.getTicketStatus())
			.as("좌석이 없어도 FAILED 상태로 정상 변경됨")
			.isEqualTo(TicketStatus.FAILED);
	}

	@Test
	@DisplayName("만료되지 않은 Draft 티켓은 처리하지 않는다")
	void expireDraftTicket_notExpired_ignored() {
		// given: 최근 생성된 Draft 티켓 (만료되지 않음)
		Ticket ticket = ticketHelper.createDraftTicket(user, seat, event);

		// when: 스케줄러 실행 (Lock 없이)
		scheduler.expireDraftTicketsInternal();

		// then: 티켓 상태가 변경되지 않음
		Ticket notExpiredTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(notExpiredTicket.getTicketStatus()).isEqualTo(TicketStatus.DRAFT);
	}

	@Test
	@Transactional
	@DisplayName("PAID 상태의 티켓은 처리하지 않는다")
	void expireDraftTicket_paidTicket_ignored() throws Exception {
		// given: 만료 시간이 지난 PAID 티켓
		Ticket ticket = ticketHelper.createPaidTicket(user, seat, event);
		setCreatedAt(ticket, LocalDateTime.now().minusMinutes(10));

		// 트랜잭션 커밋하여 스케줄러가 데이터를 볼 수 있도록 함
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// when: 스케줄러 실행 (Lock 없이, 별도 트랜잭션)
		scheduler.expireDraftTicketsInternal();

		// then: 새 트랜잭션 시작하여 결과 확인
		TestTransaction.start();
		Ticket unchangedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(unchangedTicket.getTicketStatus()).isEqualTo(TicketStatus.PAID);
	}

	@Test
	@Transactional
	@DisplayName("여러 개의 만료된 Draft 티켓을 배치로 처리한다")
	void expireDraftTicket_multipleTickets_success() throws Exception {
		// given: 3개의 만료된 Draft 티켓 생성
		Seat seat2 = seatHelper.createSeat(event, "A2", SeatGrade.VIP);
		Seat seat3 = seatHelper.createSeat(event, "A3", SeatGrade.VIP);

		Ticket ticket1 = ticketHelper.createDraftTicket(user, seat, event);
		Ticket ticket2 = ticketHelper.createDraftTicket(user, seat2, event);
		Ticket ticket3 = ticketHelper.createDraftTicket(user, seat3, event);

		setCreatedAt(ticket1, LocalDateTime.now().minusMinutes(10));
		setCreatedAt(ticket2, LocalDateTime.now().minusMinutes(10));
		setCreatedAt(ticket3, LocalDateTime.now().minusMinutes(10));

		// 좌석들을 RESERVED 상태로 변경
		seat.markAsReserved();
		seat2.markAsReserved();
		seat3.markAsReserved();
		seatRepository.save(seat);
		seatRepository.save(seat2);
		seatRepository.save(seat3);

		// 트랜잭션 커밋하여 스케줄러가 데이터를 볼 수 있도록 함
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// when: 스케줄러 실행 (Lock 없이, 별도 트랜잭션)
		scheduler.expireDraftTicketsInternal();

		// then: 새 트랜잭션 시작하여 결과 확인
		TestTransaction.start();
		assertThat(ticketRepository.findById(ticket1.getId()).orElseThrow().getTicketStatus())
			.isEqualTo(TicketStatus.FAILED);
		assertThat(ticketRepository.findById(ticket2.getId()).orElseThrow().getTicketStatus())
			.isEqualTo(TicketStatus.FAILED);
		assertThat(ticketRepository.findById(ticket3.getId()).orElseThrow().getTicketStatus())
			.isEqualTo(TicketStatus.FAILED);

		// then: 모든 좌석이 AVAILABLE 상태로 변경됨
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus())
			.isEqualTo(SeatStatus.AVAILABLE);
		assertThat(seatRepository.findById(seat2.getId()).orElseThrow().getSeatStatus())
			.isEqualTo(SeatStatus.AVAILABLE);
		assertThat(seatRepository.findById(seat3.getId()).orElseThrow().getSeatStatus())
			.isEqualTo(SeatStatus.AVAILABLE);
	}

	@Test
	@Transactional
	@DisplayName("좌석이 없는 티켓과 있는 티켓이 섞여있어도 정상 처리한다")
	void expireDraftTicket_mixedTickets_success() throws Exception {
		// given: 좌석 있는 티켓과 없는 티켓 혼합
		Ticket ticketWithSeat = ticketHelper.createDraftTicket(user, seat, event);
		Ticket ticketWithoutSeat = Ticket.builder()
			.owner(user)
			.event(event)
			.seat(null)
			.ticketStatus(TicketStatus.DRAFT)
			.build();
		ticketRepository.save(ticketWithoutSeat);

		setCreatedAt(ticketWithSeat, LocalDateTime.now().minusMinutes(10));
		setCreatedAt(ticketWithoutSeat, LocalDateTime.now().minusMinutes(10));

		seat.markAsReserved();
		seatRepository.save(seat);

		// 트랜잭션 커밋하여 스케줄러가 데이터를 볼 수 있도록 함
		TestTransaction.flagForCommit();
		TestTransaction.end();

		// when: 스케줄러 실행 (Lock 없이, 별도 트랜잭션)
		scheduler.expireDraftTicketsInternal();

		// then: 새 트랜잭션 시작하여 결과 확인
		TestTransaction.start();
		assertThat(ticketRepository.findById(ticketWithSeat.getId()).orElseThrow().getTicketStatus())
			.isEqualTo(TicketStatus.FAILED);
		assertThat(ticketRepository.findById(ticketWithoutSeat.getId()).orElseThrow().getTicketStatus())
			.isEqualTo(TicketStatus.FAILED);

		// then: 좌석 있던 티켓의 좌석만 AVAILABLE로 변경됨
		assertThat(seatRepository.findById(seat.getId()).orElseThrow().getSeatStatus())
			.isEqualTo(SeatStatus.AVAILABLE);
	}

	// ========== Helper Methods ==========

	/**
	 * Reflection을 사용하여 createAt 필드를 수정
	 */
	private void setCreatedAt(Ticket ticket, LocalDateTime time) throws Exception {
		Field createAtField = ticket.getClass().getSuperclass().getDeclaredField("createAt");
		createAtField.setAccessible(true);
		createAtField.set(ticket, time);
		ticketRepository.saveAndFlush(ticket);
	}
}