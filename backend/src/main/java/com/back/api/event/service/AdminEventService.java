package com.back.api.event.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.AdminEventDashboardResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegisterStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.seat.entity.SeatStatus;
import com.back.domain.seat.repository.SeatRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventService {

	private final EventService eventService;
	private final EventRepository eventRepository;
	private final PreRegisterRepository preRegisterRepository;
	private final SeatRepository seatRepository;

	@Transactional
	public EventResponse createEvent(EventCreateRequest request) {
		return eventService.createEvent(request);
	}

	@Transactional
	public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {
		return eventService.updateEvent(eventId, request);
	}

	@Transactional
	public void deleteEvent(Long eventId) {
		eventService.deleteEvent(eventId);
	}

	public List<AdminEventDashboardResponse> getAllEventsDashboard() {
		List<Event> events = eventRepository.findAll();

		return events.stream()
			.map(event -> {
				Long eventId = event.getId();

				// 1. 이벤트별 현재 사전등록 인원 수 조회
				Long preRegisterCount = preRegisterRepository.countByEvent_IdAndPreRegisterStatus(
					eventId,
					PreRegisterStatus.REGISTERED
				);

				// 2. 이벤트별 총 판매 좌석 조회 (SOLD 상태인 좌석)
				Long totalSoldSeats = seatRepository.countByEventIdAndSeatStatus(eventId, SeatStatus.SOLD);

				// 3. 이벤트별 총 판매 금액 조회
				Long totalSalesAmount = seatRepository.sumPriceByEventIdAndSeatStatus(eventId, SeatStatus.SOLD);

				return AdminEventDashboardResponse.of(
					event.getId(),
					event.getTitle(),
					event.getStatus(),
					preRegisterCount,
					totalSoldSeats,
					totalSalesAmount != null ? totalSalesAmount : 0L
				);
			})
			.collect(Collectors.toList());
	}
}
