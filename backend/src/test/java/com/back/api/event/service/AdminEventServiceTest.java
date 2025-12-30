package com.back.api.event.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import com.back.support.factory.EventFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminEventService 단위 테스트")
class AdminEventServiceTest {

	@InjectMocks
	private AdminEventService adminEventService;

	@Mock
	private EventService eventService;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private PreRegisterRepository preRegisterRepository;

	@Mock
	private SeatRepository seatRepository;

	@Nested
	@DisplayName("이벤트 생성")
	class CreateEvent {

		@Test
		@DisplayName("EventService를 호출하여 이벤트를 생성한다")
		void createEvent_Success() {
			// given
			EventCreateRequest request = mock(EventCreateRequest.class);
			EventResponse expectedResponse = mock(EventResponse.class);

			given(eventService.createEvent(request)).willReturn(expectedResponse);

			// when
			EventResponse result = adminEventService.createEvent(request);

			// then
			assertThat(result).isEqualTo(expectedResponse);
			verify(eventService).createEvent(request);
		}
	}

	@Nested
	@DisplayName("이벤트 수정")
	class UpdateEvent {

		@Test
		@DisplayName("EventService를 호출하여 이벤트를 수정한다")
		void updateEvent_Success() {
			// given
			Long eventId = 1L;
			EventUpdateRequest request = mock(EventUpdateRequest.class);
			EventResponse expectedResponse = mock(EventResponse.class);

			given(eventService.updateEvent(eventId, request)).willReturn(expectedResponse);

			// when
			EventResponse result = adminEventService.updateEvent(eventId, request);

			// then
			assertThat(result).isEqualTo(expectedResponse);
			verify(eventService).updateEvent(eventId, request);
		}
	}

	@Nested
	@DisplayName("이벤트 삭제")
	class DeleteEvent {

		@Test
		@DisplayName("EventService를 호출하여 이벤트를 삭제한다")
		void deleteEvent_Success() {
			// given
			Long eventId = 1L;

			// when
			adminEventService.deleteEvent(eventId);

			// then
			verify(eventService).deleteEvent(eventId);
		}
	}

	@Nested
	@DisplayName("전체 이벤트 대시보드 조회")
	class GetAllEventsDashboard {

		@Test
		@DisplayName("모든 이벤트의 대시보드 정보를 조회한다")
		void getAllEventsDashboard_Success() {
			// given
			Event event1 = EventFactory.fakeEvent("이벤트1");
			Event event2 = EventFactory.fakeEvent("이벤트2");
			List<Event> events = List.of(event1, event2);

			Pageable pageable = PageRequest.of(0, 20);
			Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());


			given(eventRepository.findAll(any(Pageable.class))).willReturn(eventPage);  // <- 변경!
			given(preRegisterRepository.countByEvent_IdAndPreRegisterStatus(
				any(), eq(PreRegisterStatus.REGISTERED))).willReturn(10L);
			given(seatRepository.countByEventIdAndSeatStatus(any(), eq(SeatStatus.SOLD))).willReturn(5L);
			given(seatRepository.sumPriceByEventIdAndSeatStatus(any(), eq(SeatStatus.SOLD))).willReturn(50000L);

			// when
			Page<AdminEventDashboardResponse> results = adminEventService.getAllEventsDashboard(0, 20);

			// then
			assertThat(results.getContent()).hasSize(2);
			assertThat(results.getTotalElements()).isEqualTo(2);
			assertThat(results.getTotalPages()).isEqualTo(1);
			assertThat(results.getSize()).isEqualTo(20);
			assertThat(results.getNumber()).isEqualTo(0);

			assertThat(results.getContent().get(0).preRegisterCount()).isEqualTo(10L);
			assertThat(results.getContent().get(0).totalSoldSeats()).isEqualTo(5L);
			assertThat(results.getContent().get(0).totalSalesAmount()).isEqualTo(50000L);
		}

		@Test
		@DisplayName("이벤트가 없으면 빈 리스트를 반환한다")
		void getAllEventsDashboard_EmptyList() {
			// given
			Pageable pageable = PageRequest.of(0, 20);
			Page<Event> emptyPage = new PageImpl<>(List.of(), pageable, 0);

			given(eventRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

			// when
			Page<AdminEventDashboardResponse> results = adminEventService.getAllEventsDashboard(0, 20);

			// then
			assertThat(results.getContent()).isEmpty();
			assertThat(results.getTotalElements()).isEqualTo(0);
			assertThat(results.getTotalPages()).isEqualTo(0);
		}

		@Test
		@DisplayName("총 판매 금액이 null이면 0을 반환한다")
		void getAllEventsDashboard_NullSalesAmount() {
			// given
			Event event = EventFactory.fakeEvent("이벤트");
			Pageable pageable = PageRequest.of(0, 20);
			Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);

			given(eventRepository.findAll(any(Pageable.class))).willReturn(eventPage);
			given(preRegisterRepository.countByEvent_IdAndPreRegisterStatus(
				any(), eq(PreRegisterStatus.REGISTERED))).willReturn(0L);
			given(seatRepository.countByEventIdAndSeatStatus(any(), eq(SeatStatus.SOLD))).willReturn(0L);
			given(seatRepository.sumPriceByEventIdAndSeatStatus(any(), eq(SeatStatus.SOLD))).willReturn(null);

			// when
			Page<AdminEventDashboardResponse> results = adminEventService.getAllEventsDashboard(0, 20);

			// then
			assertThat(results.getContent()).hasSize(1);
			assertThat(results.getContent().get(0).totalSalesAmount()).isEqualTo(0L);
		}
	}
}
