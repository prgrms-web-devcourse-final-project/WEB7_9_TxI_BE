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
@Order(3)  // User(1) -> Event(2) 다음에 실행
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

		// Event #3 (디즈니 팝업)의 좌석 생성
		Event event3 = eventRepository.findById(3L).orElse(null);
		if (event3 == null) {
			log.warn("Event #3을 찾을 수 없습니다. EventDataInit을 먼저 실행해주세요.");
			return;
		}

		// 좌석 생성 (200석)
		List<Seat> seats = createSeatsForEvent(event3, 200);
		seatRepository.saveAll(seats);

		log.info("Seat 초기 데이터 {}석이 생성되었습니다.", seats.size());
	}

	/**
	 * 이벤트별 좌석 생성
	 * - VIP: 20석 (150,000원)
	 * - R석: 50석 (100,000원)
	 * - S석: 60석 (70,000원)
	 * - A석: 70석 (50,000원)
	 */
	private List<Seat> createSeatsForEvent(Event event, int totalSeats) {
		List<Seat> seats = new ArrayList<>();

		// VIP 20석
		int vipCount = 20;
		for (int i = 1; i <= vipCount; i++) {
			Seat seat = Seat.createSeat(
				event,
				"VIP-" + i,
				SeatGrade.VIP,
				150000
			);
			seats.add(seat);
		}

		// R석 50석
		int rCount = 50;
		for (int i = 1; i <= rCount; i++) {
			Seat seat = Seat.createSeat(
				event,
				"R-" + i,
				SeatGrade.R,
				100000
			);
			seats.add(seat);
		}

		// S석 60석
		int sCount = 60;
		for (int i = 1; i <= sCount; i++) {
			Seat seat = Seat.createSeat(
				event,
				"S-" + i,
				SeatGrade.S,
				70000
			);
			seats.add(seat);
		}

		// A석 70석
		int aCount = 70;
		for (int i = 1; i <= aCount; i++) {
			Seat seat = Seat.createSeat(
				event,
				"A-" + i,
				SeatGrade.A,
				50000
			);
			seats.add(seat);
		}

		return seats;
	}
}