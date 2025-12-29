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
@Order(4)
public class TicketDataInit implements ApplicationRunner {

	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (ticketRepository.count() > 0) {
			log.info("Ticket 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Ticket 초기 데이터를 생성합니다.");

		List<User> users = userRepository.findAll();
		List<Seat> seats = seatRepository.findAll();
		List<Event> events = eventRepository.findAll();

		log.info("User 수: {}, Seat 수: {}, Event 수: {}", users.size(), seats.size(), events.size());

		if (users.isEmpty() || seats.isEmpty() || events.isEmpty()) {
			log.warn("User, Seat, Event 데이터가 부족하여 Ticket 초기화를 건너뜁니다.");
			return;
		}

		List<Ticket> tickets = new ArrayList<>();

		// 테스트용 DRAFT 티켓 생성 (주문 테스트용)
		// test1@test.com (userId 1번) 사용자에게 DRAFT 티켓 생성
		User testUser1 = users.stream()
			.filter(u -> u.getEmail().equals("test1@test.com"))
			.findFirst()
			.orElse(null);

		if (testUser1 == null) {
			log.warn("test1@test.com 사용자를 찾을 수 없습니다.");
			return;
		}

		log.info("test1@test.com 사용자 ID: {}", testUser1.getId());

		Event firstEvent = events.get(0);
		log.info("첫 번째 이벤트 ID: {}, 제목: {}", firstEvent.getId(), firstEvent.getTitle());

		// VIP 좌석 1개에 대한 DRAFT 티켓 (Event ID로 비교)
		Seat vipSeat = seats.stream()
			.filter(s -> s.getEvent().getId().equals(firstEvent.getId())
				&& s.getSeatCode().equals("VIP-1"))
			.findFirst()
			.orElse(null);

		if (vipSeat == null) {
			log.warn("VIP-1 좌석을 찾을 수 없습니다. Event ID: {}", firstEvent.getId());
			return;
		}

		log.info("VIP-1 좌석 찾음 - ID: {}, 가격: {}", vipSeat.getId(), vipSeat.getPrice());

		Ticket ticket = Ticket.builder()
			.owner(testUser1)
			.seat(vipSeat)
			.event(firstEvent)
			.ticketStatus(TicketStatus.DRAFT)
			.build();

		tickets.add(ticket);
		ticketRepository.saveAll(tickets);

		log.info("Ticket 초기 데이터 {}개가 생성되었습니다.", tickets.size());
		log.info("테스트용 DRAFT 티켓이 test1@test.com (userId: {})에게 할당되었습니다.", testUser1.getId());
	}
}