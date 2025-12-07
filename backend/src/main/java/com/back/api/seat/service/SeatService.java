package com.back.api.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {

	private final SeatRepository seatRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/events/{eventId}/seats
	 */
	@Transactional(readOnly = true)
	public List<Seat> getSeatsByEvent(Long eventId) {
		return seatRepository.findByEventId(eventId);
	}

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/seats/{seatId}/select
	 */
	public Seat selectSeat(Long eventId, Long seatId, Long userId) {

		if (!queueEntryRedisRepository.isInEnteredQueue(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		seat.markAsReserved();

		return seatRepository.save(seat);
	}

	public Seat confirmPurchase(Long seatId, Long userId) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		// 좌석을 SOLD 상태로 변경
		seat.markAsSold();

		return seatRepository.save(seat);
	}
}
