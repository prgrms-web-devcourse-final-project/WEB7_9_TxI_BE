package com.back.api.seat.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.seat.dto.request.AutoCreateSeatsRequest;
import com.back.api.seat.dto.request.SeatCreateRequest;
import com.back.api.seat.dto.request.SeatUpdateRequest;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
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

		// (eventId, grade, seatCode) 기준 중복 검증
		validateDuplicateSeats(eventId, requests);

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

		validateDuplicateSeats(eventId, List.of(request));

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

		List<SeatCreateRequest> seatRequests = new ArrayList<>();

		// 좌석 코드 생성 (예: A1, A2, B1, B2 ...)
		for (int row = 0; row < request.rows(); row++) {
			char rowChar = (char)('A' + row);

			for (int col = 1; col <= request.cols(); col++) {
				String code = rowChar + String.valueOf(col);
				seatRequests.add(
					new SeatCreateRequest(
						code,
						request.defaultGrade(),
						request.defaultPrice()
					)
				);
			}
		}

		// 한 번의 공통 검증 로직 호출
		validateDuplicateSeats(eventId, seatRequests);

		List<Seat> seats = seatRequests.stream()
			.map(req -> createSeatEntity(event, req))
			.toList();

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

		// 수정 시에도 (eventId, grade, seatCode) 충돌 여부 체크 (자기 자신 제외)
		validateDuplicateSeatsOnUpdate(seat, request);

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

	/**
	 * (eventId, grade, seatCode) 기준 중복 검증
	 * - 요청 안에서의 중복
	 * - DB 안의 기존 데이터와의 중복
	 */
	private void validateDuplicateSeats(Long eventId, List<SeatCreateRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return;
		}

		// 1) 요청 내부 중복 체크
		Set<String> seen = new HashSet<>();
		Set<String> internalDuplicates = new HashSet<>();

		for (SeatCreateRequest req : requests) {
			String key = req.grade() + ":" + req.seatCode();
			if (!seen.add(key)) {
				internalDuplicates.add(key);
			}
		}

		if (!internalDuplicates.isEmpty()) {
			throw new ErrorException(
				"요청 내 중복 좌석: " + internalDuplicates,
				SeatErrorCode.DUPLICATE_SEAT_CODE
			);
		}

		// 2) DB 중복 체크 (grade별 그룹핑 후 한 번에 조회)
		Map<SeatGrade, List<String>> seatsByGrade = requests.stream()
			.collect(Collectors.groupingBy(
				SeatCreateRequest::grade,
				Collectors.mapping(SeatCreateRequest::seatCode, Collectors.toList())
			));

		List<String> dbDuplicates = new ArrayList<>();

		for (Map.Entry<SeatGrade, List<String>> entry : seatsByGrade.entrySet()) {
			List<String> existingCodes = seatRepository.findExistingSeatCodes(
				eventId,
				entry.getKey(),
				entry.getValue()
			);

			existingCodes.forEach(code -> dbDuplicates.add(entry.getKey() + ":" + code));
		}

		if (!dbDuplicates.isEmpty()) {
			throw new ErrorException(
				"이미 존재하는 좌석: " + dbDuplicates,
				SeatErrorCode.DUPLICATE_SEAT_CODE
			);
		}
	}

	/**
	 * 수정 시 중복 검증
	 * - 변경된 (grade, seatCode) 조합이 DB에 이미 존재하는지 확인
	 * - changed 체크를 통해 불필요한 DB 조회 최소화
	 */
	private void validateDuplicateSeatsOnUpdate(Seat seat, SeatUpdateRequest request) {
		boolean changed = !seat.getSeatCode().equals(request.seatCode())
			|| seat.getGrade() != request.grade();

		if (!changed)
			return;

		List<String> existing = seatRepository.findExistingSeatCodesExceptSelf(
			seat.getEvent().getId(),
			request.grade(),
			request.seatCode(),
			seat.getId()
		);

		if (!existing.isEmpty()) {
			throw new ErrorException("이미 존재하는 좌석: "
				+ request.grade() + ":" + request.seatCode(),
				SeatErrorCode.DUPLICATE_SEAT_CODE);
		}
	}
}
