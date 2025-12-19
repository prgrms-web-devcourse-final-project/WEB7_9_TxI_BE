package com.back.global.init.perf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("perf")
public class PerfSeatDataInitializer {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;

	public void init() {
		if (seatRepository.count() > 0) {
			log.info("Seat 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		int totalSeats = 0;

		// Event #3 (OPEN 상태 콘서트): 500석 생성
		Event event3 = eventRepository.findById(3L).orElse(null);
		if (event3 == null) {
			log.warn("Event #3을 찾을 수 없습니다.");
		} else {
			log.info("Seat 초기 데이터 생성 중: Event #3 ({}) - 500석 (VIP-A 50석, R-C 100석, S-B 150석, A-D 200석)",
				event3.getTitle());

			List<Seat> seats3 = createSeatsForEvent3and4(event3);
			seatRepository.saveAll(seats3);
			totalSeats += seats3.size();

			log.info("✅ Event #3 Seat 데이터 생성 완료: {}석 (티켓팅 경쟁 부하테스트용)", seats3.size());
		}

		// Event #4 (CLOSED 상태 콘서트): 100석 생성
		Event event4 = eventRepository.findById(4L).orElse(null);
		if (event4 == null) {
			log.warn("Event #4를 찾을 수 없습니다.");
		} else {
			log.info("Seat 초기 데이터 생성 중: Event #4 ({}) - 100석 (VIP-E 20석, R-G 30석, S-F 30석, A-H 20석)",
				event4.getTitle());

			List<Seat> seats4 = createSeatsForEvent3and4(event4);
			seatRepository.saveAll(seats4);
			totalSeats += seats4.size();

			log.info("✅ Event #4 Seat 데이터 생성 완료: {}석 (티켓 조회/관리 부하테스트용)", seats4.size());
		}

		log.info("✅ Seat 데이터 생성 완료: 총 {}석 (Event #3: 500석, Event #4: 100석)", totalSeats);
	}

	/**
	 * Event #3용 좌석 생성
	 * - 총 500석
	 * - VIP: A1~A50 (50석)
	 * - R: C1~C100 (100석)
	 * - S: B1~B150 (150석)
	 * - A: D1~D200 (200석)
	 */
	private List<Seat> createSeatsForEvent3and4(Event event) {
		List<Seat> seats = new ArrayList<>();

		// VIP: A1 ~ A50 (50석)
		for (int i = 1; i <= 50; i++) {
			seats.add(Seat.createSeat(event, "A" + i, SeatGrade.VIP, event.getMaxPrice()));
		}

		// R: C1 ~ C100 (100석)
		for (int i = 1; i <= 100; i++) {
			seats.add(Seat.createSeat(event, "C" + i, SeatGrade.R, event.getMaxPrice() - 20000));
		}

		// S: B1 ~ B150 (150석)
		for (int i = 1; i <= 150; i++) {
			seats.add(Seat.createSeat(event, "B" + i, SeatGrade.S, event.getMinPrice() + 30000));
		}

		// A: D1 ~ D200 (200석)
		for (int i = 1; i <= 200; i++) {
			seats.add(Seat.createSeat(event, "D" + i, SeatGrade.A, event.getMinPrice()));
		}

		return seats;
	}
}
