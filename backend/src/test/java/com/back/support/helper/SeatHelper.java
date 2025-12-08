package com.back.support.helper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.repository.SeatRepository;
import com.back.support.factory.SeatFactory;

@Component
public class SeatHelper {

	private final SeatRepository seatRepository;

	public SeatHelper(SeatRepository seatRepository) {
		this.seatRepository = seatRepository;
	}

	public Seat createSeat(Event event) {
		return seatRepository.save(SeatFactory.fakeSeat(event));
	}

	public Seat createSeat(Event event, String seatCode) {
		return seatRepository.save(SeatFactory.fakeSeat(event, seatCode));
	}

	public Seat createSeat(Event event, SeatGrade grade) {
		return seatRepository.save(SeatFactory.fakeSeat(event, grade));
	}

	public Seat createSeat(Event event, String seatCode, SeatGrade grade) {
		return seatRepository.save(SeatFactory.fakeSeat(event, seatCode, grade));
	}

	public Seat createSeat(Event event, String seatCode, SeatGrade grade, int price) {
		return seatRepository.save(SeatFactory.fakeSeat(event, seatCode, grade, price));
	}

	public List<Seat> createSeats(Event event, int count) {
		return seatRepository.saveAll(
			java.util.stream.IntStream.range(0, count)
				.mapToObj(i -> SeatFactory.fakeSeat(event))
				.toList()
		);
	}

	public void clearSeat() {
		seatRepository.deleteAll();
	}
}