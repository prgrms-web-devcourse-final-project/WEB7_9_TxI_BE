package com.back.api.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

	private final SeatRepository seatRepository;

	// ===== 일반 사용자용 API =====

	/**
	 * 이벤트의 좌석 목록 조회
	 * GET /api/events/{eventId}/seats
	 */
	public List<Seat> getSeatsByEvent(Long eventId) {
		return seatRepository.findByEventId(eventId);
	}

	/**
	 * 좌석 선택 (예약/구매)
	 * POST /api/seats/{seatId}/select
	 */
	@Transactional
	public Seat selectSeat(Long seatId, Long userId) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

		// 좌석을 RESERVED 상태로 변경 (또는 SOLD로 변경)
		// 실제 구현 시 userId를 저장하는 로직 추가 필요
		seat.markAsReserved();

		return seatRepository.save(seat);
	}

}
