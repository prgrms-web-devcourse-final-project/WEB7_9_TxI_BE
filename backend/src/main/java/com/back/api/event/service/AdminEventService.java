package com.back.api.event.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.back.global.error.code.AuthErrorCode;
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
	public EventResponse createEvent(EventCreateRequest request, long storeId) {
		return eventService.createEvent(request, storeId);
	}

	@Transactional
	public EventResponse updateEvent(Long eventId, long storeId, EventUpdateRequest request) {
		return eventService.updateEvent(eventId, storeId, request);
	}

	@Transactional
	public void deleteEvent(Long eventId, Long storeId) {
		eventService.deleteEvent(eventId, storeId);
	}

	public Page<AdminEventDashboardResponse> getAllEventsDashboard(int page, int size, long storeId) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Event> eventPage = eventRepository.findAllByStore_Id(pageable, storeId);

		return eventPage.map(event -> {
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
				totalSalesAmount != null ? totalSalesAmount : 0L,
				event.isDeleted()
			);
		});
	}

	@Transactional(readOnly = true)
	public EventResponse getEventForAdmin(Long eventId, Long storeId) {
		Event event = eventService.findEventById(eventId);
		if (!event.getStore().getId().equals(storeId)) {
			throw new ErrorException(AuthErrorCode.FORBIDDEN);
		}
		return eventService.getEvent(eventId);
	}
}
