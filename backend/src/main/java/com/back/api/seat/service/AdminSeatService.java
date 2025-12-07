package com.back.api.seat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.dto.request.AutoCreateSeatsRequest;
import com.back.api.seat.dto.request.SeatCreateRequest;
import com.back.api.seat.dto.request.SeatUpdateRequest;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSeatService {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	// ===== 관리자용 API =====

	/**
	 * 좌석 대량 생성
	 * POST /api/admin/events/{eventId}/seats
	 */
	@Transactional
	public List<Seat> bulkCreateSeats(Long eventId, List<SeatCreateRequest> requests) {
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_EVENT));

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
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_EVENT));

		Seat seat = createSeatEntity(event, request);

		return seatRepository.save(seat);
	}

	/**
	 * 좌석 자동 생성 (행-열 기반)
	 * POST /api/admin/events/{eventId}/seats/auto
	 */
	@Transactional
	public List<Seat> autoCreateSeats(Long eventId, AutoCreateSeatsRequest request) {
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_EVENT));

		List<Seat> seats = new ArrayList<>();

		// A, B, C... 형식으로 행 생성
		for (int row = 0; row < request.rows(); row++) {
			char rowChar = (char)('A' + row);
			String rowName = String.valueOf(rowChar);

			// 1, 2, 3... 형식으로 열 생성
			for (int col = 1; col <= request.cols(); col++) {
				String seatCode = rowName + col;
				Seat seat = Seat.createSeat(event, seatCode, request.defaultGrade(), request.defaultPrice());
				seats.add(seat);
			}
		}

		return seatRepository.saveAll(seats);
	}

	/**
	 * 좌석 수정
	 * PUT /api/admin/seats/{seatId}
	 */
	@Transactional
	public Seat updateSeat(Long seatId, SeatUpdateRequest request) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new ErrorException(SeatErrorCode.NOT_FOUND_SEAT));

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
			throw new ErrorException(SeatErrorCode.NOT_FOUND_SEAT);
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

	private Seat createSeatEntity(Event event, SeatCreateRequest request) {
		return Seat.createSeat(event, request.seatCode(), request.grade(), request.price());
	}
}
