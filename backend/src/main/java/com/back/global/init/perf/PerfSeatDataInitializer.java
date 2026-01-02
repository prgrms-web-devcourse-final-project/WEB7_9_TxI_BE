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
		int seatsPerEvent = 625; // 500명 유저 x 5개 티켓 / 4개 이벤트 = 625석

		// Event #1-4: 각 이벤트마다 625석씩 생성 (총 2,500석)
		for (long eventId = 1L; eventId <= 4L; eventId++) {
			Event event = eventRepository.findById(eventId).orElse(null);
			if (event == null) {
				log.warn("Event #{}를 찾을 수 없습니다.", eventId);
				continue;
			}

			log.info("Seat 초기 데이터 생성 중: Event #{} ({}) - {}석",
				eventId, event.getTitle(), seatsPerEvent);

			List<Seat> seats = createSeatsForEvent(event, eventId, seatsPerEvent);
			seatRepository.saveAll(seats);
			totalSeats += seats.size();

			log.info("✅ Event #{} Seat 데이터 생성 완료: {}석", eventId, seats.size());
		}

		log.info("✅ Seat 데이터 생성 완료: 총 {}석 (이벤트당 {}석씩)", totalSeats, seatsPerEvent);
	}

	/**
	 * 각 이벤트용 좌석 생성
	 * - VIP: 10%
	 * - R: 30%
	 * - S: 40%
	 * - A: 20%
	 */
	private List<Seat> createSeatsForEvent(Event event, long eventId, int totalSeats) {
		List<Seat> seats = new ArrayList<>();
		String prefix = String.valueOf((char) ('A' + (eventId - 1)));

		int vipCount = (int) (totalSeats * 0.1);
		int rCount = (int) (totalSeats * 0.3);
		int sCount = (int) (totalSeats * 0.4);
		int aCount = totalSeats - vipCount - rCount - sCount;

		// VIP
		for (int i = 1; i <= vipCount; i++) {
			seats.add(Seat.createSeat(event, prefix + "VIP" + i, SeatGrade.VIP, event.getMaxPrice()));
		}

		// R
		for (int i = 1; i <= rCount; i++) {
			seats.add(Seat.createSeat(event, prefix + "R" + i, SeatGrade.R, event.getMaxPrice() - 20000));
		}

		// S
		for (int i = 1; i <= sCount; i++) {
			seats.add(Seat.createSeat(event, prefix + "S" + i, SeatGrade.S, event.getMinPrice() + 30000));
		}

		// A
		for (int i = 1; i <= aCount; i++) {
			seats.add(Seat.createSeat(event, prefix + "A" + i, SeatGrade.A, event.getMinPrice()));
		}

		return seats;
	}
}
