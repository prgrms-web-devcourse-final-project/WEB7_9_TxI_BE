package com.back.global.init.perf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventStatus;
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
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfTicketDataInitializer {

	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	public void init(double ticketRatio) {
		if (ticketRepository.count() > 0) {
			log.info("Ticket 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			log.warn("User 데이터가 없습니다. PerfUserDataInitializer를 먼저 실행해주세요.");
			return;
		}

		log.info("Ticket 초기 데이터 생성 중: Event #4에만 티켓 배정 (빠른 초기화)");

		int totalTickets = 0;

		// Event #4에만 티켓 생성 (getMyTickets, getTicketDetail 테스트용)
		// Event #1~3: 티켓 생성 건너뜀 (초기화 시간 단축)
		for (long eventId = 1L; eventId <= 4L; eventId++) {
			// Event #1, #2: 티켓 생성 건너뜀 (초기화 시간 단축)
			if (eventId == 1L || eventId == 2L) {
				log.info("Event #{} 건너뜀: 초기화 시간 단축", eventId);
				continue;
			}

			// Event #3: selectSeat 부하 테스트용 (좌석 AVAILABLE 상태 유지)
			if (eventId == 3L) {
				log.info("Event #3 건너뜀: selectSeat 부하 테스트용 이벤트 (좌석은 모두 AVAILABLE 상태 유지)");
				continue;
			}

			Event event = eventRepository.findById(eventId).orElse(null);
			if (event == null) {
				log.warn("Event #{}를 찾을 수 없습니다. 티켓 생성을 건너뜁니다.", eventId);
				continue;
			}

			// 해당 이벤트의 모든 좌석 조회
			List<Seat> seats = seatRepository.findByEventIdAndSeatStatus(
				event.getId(),
				SeatStatus.AVAILABLE
			);

			if (seats.isEmpty()) {
				log.warn("Event #{} - 사용 가능한 좌석이 없습니다. 티켓 생성을 건너뜁니다.", eventId);
				continue;
			}

			// Event #4: getMyTickets, getTicketDetail 테스트용 티켓 100개 생성
			// userId 1~100에게 각각 1장씩 배정 (순환 배정)
			int ticketCount = Math.min(100, seats.size());

			log.info("Event #{} ({})에 {}장의 티켓 생성 중...", eventId, event.getTitle(), ticketCount);

			List<Ticket> tickets = createIssuedTicketsForEvent(event, users, seats, ticketCount);
			ticketRepository.saveAll(tickets);
			totalTickets += tickets.size();

			log.info("✅ Event #{} Ticket 데이터 생성 완료: {}장 (ISSUED 상태)", eventId, tickets.size());
		}

		log.info("✅ Ticket 데이터 생성 완료: 총 {}장 (유저당 평균 {}개)",
			totalTickets, totalTickets / users.size());
	}

	/**
	 * 이벤트별 ISSUED 티켓 생성
	 * - 지정된 개수만큼 티켓 생성
	 * - 모든 티켓은 ISSUED 상태 (발급 완료)
	 * - 좌석은 SOLD 상태로 직접 설정
	 * - 사용자는 순환하여 배정
	 * - Perf 전용 생성 메서드를 사용하여 상태 전이 로직 우회
	 */
	private List<Ticket> createIssuedTicketsForEvent(Event event, List<User> users, List<Seat> seats, int ticketCount) {
		List<Ticket> tickets = new ArrayList<>();

		for (int i = 0; i < ticketCount; i++) {
			User user = users.get(i % users.size()); // 사용자 순환 배정
			Seat seat = seats.get(i);

			// 좌석 상태를 바로 SOLD로 설정 (Perf 전용 메서드 사용)
			seat.setSeatStatusForPerf(SeatStatus.SOLD);
			seatRepository.save(seat);

			// 티켓을 바로 ISSUED 상태로 생성 (Perf 전용 정적 팩토리 메서드 사용)
			Ticket ticket = Ticket.issuedForPerf(user, seat, event);

			tickets.add(ticket);
		}

		return tickets;
	}

	private List<Ticket> createTicketsForEvent(Event event, List<User> users,
		List<Seat> availableSeats, int count) {
		List<Ticket> tickets = new ArrayList<>();

		int ticketCount = Math.min(count, availableSeats.size());
		ticketCount = Math.min(ticketCount, users.size());

		for (int i = 0; i < ticketCount; i++) {
			User user = users.get(i);
			Seat seat = availableSeats.get(i);

			// 좌석 상태 변경: AVAILABLE -> SOLD
			seat.markAsReserved();
			seat.markAsSold();
			seatRepository.save(seat);

			// 티켓 생성 (다양한 상태 분포)
			Ticket ticket = createTicketWithRandomStatus(user, seat, event, i, ticketCount);
			tickets.add(ticket);
		}

		return tickets;
	}

	private Ticket createTicketWithRandomStatus(User user, Seat seat, Event event, int index, int total) {
		Ticket ticket = Ticket.builder()
			.owner(user)
			.seat(seat)
			.event(event)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		// 티켓 상태 분포:
		// - 80%: ISSUED (발급 완료)
		// - 10%: PAID (결제 완료, 발급 대기)
		// - 5%: USED (사용 완료)
		// - 5%: DRAFT (임시 생성)

		double ratio = (double) index / total;

		if (ratio < 0.80) {
			// ISSUED 상태
			ticket.markPaid();
			ticket.issue();
		} else if (ratio < 0.90) {
			// PAID 상태
			ticket.markPaid();
		} else if (ratio < 0.95) {
			// USED 상태
			ticket.markPaid();
			ticket.issue();
			ticket.markAsUsed();
		}
		// 나머지 5%는 DRAFT 상태 유지

		return ticket;
	}
}
