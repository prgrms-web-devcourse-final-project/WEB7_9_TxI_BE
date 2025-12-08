package com.back.api.seat.service;

import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/events/{eventId}/seats
	 */
	@Transactional(readOnly = true)
	public List<Seat> getSeatsByEvent(Long eventId, Long userId) {
		// queue에 입장한 사용자만 좌석 조회 가능
		if (!queueEntryRedisRepository.isInEnteredQueue(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}
		// 이벤트 존재 여부 확인
		if (!eventRepository.existsById(eventId)) {
			throw new ErrorException(SeatErrorCode.NOT_FOUND_EVENT);
		}

		return seatRepository.findByEventId(eventId);
	}

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/events/{eventId}/seats/{seatId}/select
	 */
	@Transactional
	public Seat selectSeat(Long eventId, Long seatId, Long userId) {
		// queue에 입장한 사용자만 좌석 조회 가능
		if (!queueEntryRedisRepository.isInEnteredQueue(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		try {
			Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
				.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

			seat.markAsReserved();

			return seatRepository.save(seat);
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new ErrorException(SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}

	}

	public Seat confirmPurchase(Long eventId, Long seatId, Long userId) {
		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		// 좌석을 SOLD 상태로 변경
		seat.markAsSold();

		return seatRepository.save(seat);
	}
}
