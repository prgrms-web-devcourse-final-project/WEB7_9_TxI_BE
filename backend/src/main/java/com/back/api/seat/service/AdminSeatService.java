package com.back.api.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.dto.SeatCreateRequest;
import com.back.api.seat.dto.SeatUpdateRequest;
import com.back.domain.seat.entity.MockEvent;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSeatService {

	private final SeatRepository seatRepository;
	// ===== 관리자용 API =====

	/**
	 * 좌석 대량 생성
	 * POST /api/admin/events/{eventId}/seats
	 */
	@Transactional
	public List<Seat> bulkCreateSeats(Long eventId, List<SeatCreateRequest> requests) {
		// TODO: Event 엔티티로 변경 필요
		MockEvent event = new MockEvent(eventId, "temp", "temp");

		List<Seat> seats = requests.stream()
			.map(req -> createSeatEntity(event, req))
			.toList();

		return seatRepository.saveAll(seats);
	}

	/**
	 * 단일 좌석 생성
	 * POST /api/admin/events/{eventId}/seats/single
	 */
	@Transactional
	public Seat createSingleSeat(Long eventId, SeatCreateRequest request) {
		// TODO: Event 엔티티로 변경 필요
		MockEvent event = new MockEvent(eventId, "temp", "temp");

		Seat seat = createSeatEntity(event, request);

		return seatRepository.save(seat);
	}

	/**
	 * 좌석 수정
	 * PUT /api/admin/seats/{seatId}
	 */
	@Transactional
	public Seat updateSeat(Long seatId, SeatUpdateRequest request) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

		seat.update(request.seatCode(), request.grade(), request.price(), request.seatStatus());

		return seatRepository.save(seat);
	}

	/**
	 * 단일 좌석 삭제
	 * DELETE /api/admin/seats/{seatId}
	 */
	@Transactional
	public void deleteSeat(Long seatId) {
		if (!seatRepository.existsById(seatId)) {
			throw new IllegalArgumentException("Seat not found: " + seatId);
		}
		seatRepository.deleteById(seatId);
	}

	/**
	 * 이벤트의 모든 좌석 삭제
	 * DELETE /api/admin/events/{eventId}/seats
	 */
	@Transactional
	public void deleteAllEventSeats(Long eventId) {
		seatRepository.deleteByEventId(eventId);
	}

	// ===== Private Helper Methods =====

	private Seat createSeatEntity(MockEvent event, SeatCreateRequest request) {
		return Seat.createSeat(event, request.seatCode(), request.grade(), request.price());
	}
}
