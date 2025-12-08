package com.back.support.factory;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;

public class SeatFactory extends BaseFactory {

	public static Seat fakeSeat(Event event) {
		return Seat.createSeat(
			event,
			faker.regexify("[A-Z][0-9]{1,2}"),
			faker.options().option(SeatGrade.values()),
			faker.number().numberBetween(50000, 200000)
		);
	}

	public static Seat fakeSeat(Event event, String seatCode) {
		return Seat.createSeat(
			event,
			seatCode,
			faker.options().option(SeatGrade.values()),
			faker.number().numberBetween(50000, 200000)
		);
	}

	public static Seat fakeSeat(Event event, SeatGrade grade) {
		return Seat.createSeat(
			event,
			faker.regexify("[A-Z][0-9]{1,2}"),
			grade,
			faker.number().numberBetween(50000, 200000)
		);
	}

	public static Seat fakeSeat(Event event, String seatCode, SeatGrade grade) {
		return Seat.createSeat(
			event,
			seatCode,
			grade,
			faker.number().numberBetween(50000, 200000)
		);
	}

	public static Seat fakeSeat(Event event, String seatCode, SeatGrade grade, int price) {
		return Seat.createSeat(event, seatCode, grade, price);
	}
}