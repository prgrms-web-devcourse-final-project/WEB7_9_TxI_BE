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
import com.back.global.observability.metrics.BusinessMetrics;

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
	private final BusinessMetrics businessMetrics;

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
	// 원자적 업데이트로 동시성 제어
	@Transactional
	public Seat reserveSeat(Long eventId, Long seatId, Long userId) {
		// AVAILABLE -> RESERVED를 원자적으로 시도
		int updated = seatRepository.updateSeatStatusIfMatch(
			eventId, seatId,
			SeatStatus.AVAILABLE, SeatStatus.RESERVED
		);

		if (updated == 0) {
			// 동시성 충돌 발생 (CAS 실패)
			businessMetrics.seatConcurrencyConflict(eventId);

			// 실패: 좌석이 없거나, 이미 다른 상태로 변경됨
			Seat current = seatRepository.findByEventIdAndId(eventId, seatId)
				.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

			if (current.getSeatStatus() == SeatStatus.SOLD) {
				businessMetrics.seatSelectionFailure(eventId, "ALREADY_SOLD");
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_SOLD);
			}
			if (current.getSeatStatus() == SeatStatus.RESERVED) {
				businessMetrics.seatSelectionFailure(eventId, "ALREADY_RESERVED");
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED);
			}
			businessMetrics.seatSelectionFailure(eventId, "CONCURRENCY_FAILURE");
			throw new ErrorException(SeatErrorCode.SEAT_CONCURRENCY_FAILURE);
		}

		// 성공: 좌석 조회 후 이벤트 발행
		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		SeatStatusMessage message = new SeatStatusMessage(
			eventId,
			seat.getId(),
			seat.getSeatCode(),
			seat.getSeatStatus().name(),
			seat.getPrice(),
			seat.getGrade().name()
		);

		eventPublisher.publishEvent(message);

		// 좌석 선택 성공 메트릭
		businessMetrics.seatSelectionSuccess(eventId);

		return seat;
	}

	// 좌석을 SOLD 상태로 변경 (결제 완료 시)
	// RESERVED -> SOLD 원자적 업데이트
	@Transactional
	public void markSeatAsSold(Long eventId, Long seatId) {
		// RESERVED -> SOLD를 원자적으로 시도
		int updated = seatRepository.updateSeatStatusIfMatch(
			eventId, seatId,
			SeatStatus.RESERVED, SeatStatus.SOLD
		);

		if (updated == 0) {
			// 실패: 실패 원인 구분
			Seat current = seatRepository.findByEventIdAndId(eventId, seatId)
				.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

			if (current.getSeatStatus() == SeatStatus.SOLD) {
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_SOLD);
			}
			throw new ErrorException(SeatErrorCode.SEAT_STATUS_TRANSITION);
		}

		// 성공: 좌석 조회 후 이벤트 발행
		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		SeatStatusMessage message = new SeatStatusMessage(
			eventId,
			seat.getId(),
			seat.getSeatCode(),
			seat.getSeatStatus().name(),
			seat.getPrice(),
			seat.getGrade().name()
		);

		eventPublisher.publishEvent(message);
	}

	// 예약 취소 또는 결제 실패 시
	// RESERVED -> AVAILABLE 원자적 업데이트
	@Transactional
	public void markSeatAsAvailable(Long eventId, Long seatId) {
		// RESERVED -> AVAILABLE를 원자적으로 시도
		int updated = seatRepository.updateSeatStatusIfMatch(
			eventId, seatId,
			SeatStatus.RESERVED, SeatStatus.AVAILABLE
		);

		if (updated == 0) {
			// 실패: 실패 원인 구분
			Seat current = seatRepository.findByEventIdAndId(eventId, seatId)
				.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

			if (current.getSeatStatus() == SeatStatus.SOLD) {
				throw new ErrorException(SeatErrorCode.SEAT_ALREADY_SOLD);
			}
			if (current.getSeatStatus() == SeatStatus.AVAILABLE) {
				// 이미 AVAILABLE이면 무시 (멱등성)
				return;
			}
			throw new ErrorException(SeatErrorCode.SEAT_STATUS_TRANSITION);
		}

		// 성공: 좌석 조회 후 이벤트 발행
		Seat seat = seatRepository.findByEventIdAndId(eventId, seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

		SeatStatusMessage message = new SeatStatusMessage(
			eventId,
			seat.getId(),
			seat.getSeatCode(),
			seat.getSeatStatus().name(),
			seat.getPrice(),
			seat.getGrade().name()
		);

		eventPublisher.publishEvent(message);
	}
}
