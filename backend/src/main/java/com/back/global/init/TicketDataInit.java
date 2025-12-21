package com.back.global.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.entity.TicketStatus;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(5)  // User(1) -> Event(2) -> Seat(3) -> QueueEntry(4) 다음에 실행
public class TicketDataInit implements ApplicationRunner {

	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (ticketRepository.count() > 0) {
			log.info("Ticket 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Ticket 초기 데이터를 생성합니다.");

		// 사용자 조회 (test1~test10)
		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. UserDataInit을 먼저 실행해주세요.");
			return;
		}

		// 이벤트 조회 (Event #3: 디즈니 팝업)
		Event event = eventRepository.findById(3L).orElse(null);
		if (event == null) {
			log.warn("Event #3을 찾을 수 없습니다. EventDataInit을 먼저 실행해주세요.");
			return;
		}

		// 사용 가능한 좌석 조회
		List<Seat> availableSeats = seatRepository.findByEventIdAndSeatStatus(
			event.getId(),
			SeatStatus.AVAILABLE
		);

		if (availableSeats.isEmpty()) {
			log.warn("Event #3에 사용 가능한 좌석이 없습니다. SeatDataInit을 먼저 실행해주세요.");
			return;
		}

		// 티켓 생성 (사용자당 10장씩, 최대 100장)
		List<Ticket> tickets = createTickets(event, users, availableSeats);
		ticketRepository.saveAll(tickets);

		log.info("Ticket 초기 데이터 {}장이 생성되었습니다.", tickets.size());
	}

	/**
	 * 티켓 생성
	 * - test1~test10 사용자에게 각각 10장씩 배정
	 * - 상태: ISSUED (80%), PAID (10%), USED (10%)
	 */
	private List<Ticket> createTickets(Event event, List<User> users, List<Seat> availableSeats) {
		List<Ticket> tickets = new ArrayList<>();

		int ticketsPerUser = 10;  // 사용자당 10장
		int maxUsers = Math.min(10, users.size());  // 최대 10명
		int maxTickets = Math.min(ticketsPerUser * maxUsers, availableSeats.size());

		for (int i = 0; i < maxTickets; i++) {
			User user = users.get(i / ticketsPerUser);  // 사용자 순환 배정
			Seat seat = availableSeats.get(i);

			// 좌석 상태 변경: AVAILABLE -> RESERVED -> SOLD
			seat.markAsReserved();
			seat.markAsSold();
			seatRepository.save(seat);

			// 티켓 생성
			Ticket ticket = createTicketWithStatus(user, seat, event, i, maxTickets);
			tickets.add(ticket);
		}

		return tickets;
	}

	/**
	 * 다양한 상태의 티켓 생성
	 * - 80%: ISSUED (발급 완료)
	 * - 10%: PAID (결제 완료)
	 * - 10%: USED (사용 완료)
	 */
	private Ticket createTicketWithStatus(User user, Seat seat, Event event, int index, int total) {
		Ticket ticket = Ticket.builder()
			.owner(user)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		double ratio = (double) index / total;

		if (ratio < 0.80) {
			// ISSUED 상태 (80%)
			ticket.markPaid();
			ticket.issue();
		} else if (ratio < 0.90) {
			// PAID 상태 (10%)
			ticket.markPaid();
		} else {
			// USED 상태 (10%)
			ticket.markPaid();
			ticket.issue();
			ticket.markAsUsed();
		}

		return ticket;
	}
}