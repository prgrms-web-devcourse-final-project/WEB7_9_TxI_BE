package com.back.api.seat.service;

import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.event.EventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 상태 변경 담당 서비스
 */
@Service
@RequiredArgsConstructor
public class SeatService {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private final QueueEntryRedisRepository queueEntryRedisRepository;
	private final EventPublisher eventPublisher;

	/**
	 * 이벤트의 좌석 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<Seat> getSeatsByEvent(Long eventId, Long userId) {
		// 이벤트 존재 여부 확인
		if (!eventRepository.existsById(eventId)) {
			throw new ErrorException(SeatErrorCode.NOT_FOUND_EVENT);
		}

		return seatRepository.findSortedSeatListByEventId(eventId);
	}

	/**
	 * 좌석 예약 (AVAILABLE -> RESERVED)
	 */
	@Transactional
	public Seat reserveSeat(Long eventId, Long seatId, Long userId) {
		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		try {
			seat.markAsReserved();

			Seat saved = seatRepository.save(seat);

			eventPublisher.publishEvent(SeatStatusMessage.from(saved));

			return saved;
		} catch (ObjectOptimisticLockingFailureException ex) {
			throw new ErrorException(SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}
	}

	/**
	 * 좌석을 SOLD 상태로 변경 (결제 완료 시)
	 */
	@Transactional
	public void markSeatAsSold(Seat seat) {
		seat.markAsSold();
		seatRepository.save(seat);
		eventPublisher.publishEvent(SeatStatusMessage.from(seat));
	}

	/**
	 * 예약 취소 또는 결제 실패 시
	 */
	@Transactional
	public void markSeatAsAvailable(Seat seat) {
		seat.markAsAvailable();
		seatRepository.save(seat);
		eventPublisher.publishEvent(SeatStatusMessage.from(seat));
	}
}
