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
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(3)
public class SeatDataInit implements ApplicationRunner {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (seatRepository.count() > 0) {
			log.info("Seat 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Seat 초기 데이터를 생성합니다.");

		List<Event> events = eventRepository.findAll();
		if (events.isEmpty()) {
			log.warn("Event 데이터가 없어 Seat 초기화를 건너뜁니다.");
			return;
		}

		List<Seat> seats = new ArrayList<>();

		// 첫 번째 이벤트에 대한 좌석 생성 (아이유 콘서트)
		Event firstEvent = events.getFirst();
		seats.addAll(createSeatsForEvent(firstEvent));

		seatRepository.saveAll(seats);

		log.info("Seat 초기 데이터 {}개가 생성되었습니다.", seats.size());
	}

	private List<Seat> createSeatsForEvent(Event event) {
		List<Seat> seats = new ArrayList<>();

		// VIP 좌석 (10개, 가장 비쌈)
		for (int i = 1; i <= 10; i++) {
			seats.add(Seat.builder()
				.event(event)
				.seatCode("VIP-" + i)
				.grade(SeatGrade.VIP)
				.price(10)
				.build());
		}

		// R석 (20개)
		for (int i = 1; i <= 20; i++) {
			seats.add(Seat.builder()
				.event(event)
				.seatCode("R-" + i)
				.grade(SeatGrade.R)
				.price(132000)
				.build());
		}

		// S석 (30개)
		for (int i = 1; i <= 30; i++) {
			seats.add(Seat.builder()
				.event(event)
				.seatCode("S-" + i)
				.grade(SeatGrade.S)
				.price(110000)
				.build());
		}

		// A석 (40개)
		for (int i = 1; i <= 40; i++) {
			seats.add(Seat.builder()
				.event(event)
				.seatCode("A-" + i)
				.grade(SeatGrade.A)
				.price(99000)
				.build());
		}

		return seats;
	}
}
