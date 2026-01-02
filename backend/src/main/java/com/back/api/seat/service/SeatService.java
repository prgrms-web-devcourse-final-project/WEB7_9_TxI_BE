package com.back.api.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.queue.service.QueueEntryReadService;
import com.back.api.seat.dto.response.SeatStatusMessage;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;
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
	private final QueueEntryReadService queueEntryReadService;
	private final EventPublisher eventPublisher;

	// 이벤트의 좌석 목록 조회
	@Transactional(readOnly = true)
	public List<Seat> getSeatsByEvent(Long eventId, Long userId, SeatGrade grade) {
		// Q ENTERED 상태인지 확인
		if (!queueEntryReadService.isUserEntered(eventId, userId)) {
			throw new ErrorException(SeatErrorCode.NOT_IN_QUEUE);
		}

		// grade가 null이면 전체 조회, 아니면 grade별 조회
		return grade == null
			? seatRepository.findAllSeatsByEventId(eventId)
			: seatRepository.findSeatsByEventIdAndGrade(eventId, grade);
	}

	// 좌석 예약 (AVAILABLE -> RESERVED)
	@Transactional
	public Seat reserveSeat(Long eventId, Long seatId, Long userId) {
		// 1) AVAILABLE -> RESERVED를 원자적으로 시도
		int updated = seatRepository.updateSeatStatusIfMatch(
			eventId, seatId,
			SeatStatus.AVAILABLE, SeatStatus.RESERVED
		);

		if (updated == 0) {
			// 2) 실패 원인 구분
			Seat current = seatRepository.findByEventIdAndId(eventId, seatId)
				.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

			if (current.getSeatStatus() == SeatStatus.SOLD) {
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_SOLD);
			}
			if (current.getSeatStatus() == SeatStatus.RESERVED) {
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED);
			}
			// 그 외 상태면 경합/선점
			throw new ErrorException(SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}

		// 3) 성공했으면 최신 상태의 Seat 반환 (이벤트 발행용)
		Seat reserved = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		eventPublisher.publishEvent(SeatStatusMessage.from(reserved));
		return reserved;
	}

	// 좌석을 SOLD 상태로 변경 (결제 완료 시)
	@Transactional
	public void markSeatAsSold(Seat seat) {
		seat.markAsSold();
		seatRepository.save(seat);
		eventPublisher.publishEvent(SeatStatusMessage.from(seat));
	}

	// 예약 취소 또는 결제 실패 시
	@Transactional
	public void markSeatAsAvailable(Seat seat) {
		seat.markAsAvailable();
		seatRepository.save(seat);
		eventPublisher.publishEvent(SeatStatusMessage.from(seat));
	}
}
