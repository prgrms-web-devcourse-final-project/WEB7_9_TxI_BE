package com.back.api.event.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@InjectMocks
	private EventService eventService;

	private LocalDateTime now;
	private LocalDateTime preOpenAt;
	private LocalDateTime preCloseAt;
	private LocalDateTime ticketOpenAt;
	private LocalDateTime ticketCloseAt;

	@BeforeEach
	void setUp() {
		now = LocalDateTime.now();
		preOpenAt = now.plusDays(1);
		preCloseAt = now.plusDays(5);
		ticketOpenAt = now.plusDays(6);
		ticketCloseAt = now.plusDays(10);
	}

	private Event createEvent(Long id, String title, EventCategory category, EventStatus status) {
		Event event = Event.builder()
			.title(title)
			.category(category)
			.description("테스트 설명")
			.place("테스트 장소")
			.imageUrl("https://example.com/image.jpg")
			.minPrice(10000)
			.maxPrice(50000)
			.preOpenAt(preOpenAt)
			.preCloseAt(preCloseAt)
			.ticketOpenAt(ticketOpenAt)
			.ticketCloseAt(ticketCloseAt)
			.maxTicketAmount(100)
			.status(status)
			.build();
		ReflectionTestUtils.setField(event, "id", id);
		return event;
	}

	@Nested
	@DisplayName("이벤트 생성")
	class CreateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 생성 성공")
		void createEventSuccess() {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt,
				ticketCloseAt,
				100
			);

			Event savedEvent = createEvent(1L, "테스트 이벤트", EventCategory.CONCERT, EventStatus.READY);
			given(eventRepository.save(any(Event.class))).willReturn(savedEvent);

			// when
			EventResponse response = eventService.createEvent(request);

			// then
			assertThat(response.id()).isEqualTo(1L);
			assertThat(response.title()).isEqualTo("테스트 이벤트");
			assertThat(response.category()).isEqualTo(EventCategory.CONCERT);
			assertThat(response.status()).isEqualTo(EventStatus.READY);
			then(eventRepository).should(times(1)).save(any(Event.class));
		}

		@Test
		@DisplayName("사전등록 시작일이 종료일보다 늦으면 예외 발생")
		void createEventFailWhenPreOpenAtAfterPreCloseAt() {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				preCloseAt,
				preOpenAt,
				ticketOpenAt,
				ticketCloseAt,
				100
			);

			// when & then
			assertThatThrownBy(() -> eventService.createEvent(request))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.INVALID_EVENT_DATE);
		}

		@Test
		@DisplayName("티켓팅 시작일이 종료일보다 늦으면 예외 발생")
		void createEventFailWhenTicketOpenAtAfterTicketCloseAt() {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				preOpenAt,
				preCloseAt,
				ticketCloseAt,
				ticketOpenAt,
				100
			);

			// when & then
			assertThatThrownBy(() -> eventService.createEvent(request))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.INVALID_EVENT_DATE);
		}

		@Test
		@DisplayName("사전등록 종료일이 티켓팅 시작일보다 늦으면 예외 발생")
		void createEventFailWhenPreCloseAtAfterTicketOpenAt() {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				preOpenAt,
				ticketOpenAt.plusDays(1),
				ticketOpenAt,
				ticketCloseAt,
				100
			);

			// when & then
			assertThatThrownBy(() -> eventService.createEvent(request))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.INVALID_EVENT_DATE);
		}
	}

	@Nested
	@DisplayName("이벤트 수정")
	class UpdateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 수정 성공")
		void updateEventSuccess() {
			// given
			Long eventId = 1L;
			Event existingEvent = createEvent(eventId, "기존 이벤트", EventCategory.CONCERT, EventStatus.READY);

			EventUpdateRequest request = new EventUpdateRequest(
				"수정된 이벤트",
				EventCategory.POPUP,
				"수정된 설명",
				"수정된 장소",
				"https://example.com/updated-image.jpg",
				20000,
				80000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt,
				ticketCloseAt,
				200,
				EventStatus.PRE_OPEN
			);

			given(eventRepository.findById(eventId)).willReturn(Optional.of(existingEvent));

			// when
			EventResponse response = eventService.updateEvent(eventId, request);

			// then
			assertThat(response.title()).isEqualTo("수정된 이벤트");
			assertThat(response.category()).isEqualTo(EventCategory.POPUP);
			assertThat(response.status()).isEqualTo(EventStatus.PRE_OPEN);
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 수정 시 예외 발생")
		void updateEventFailWhenEventNotFound() {
			// given
			Long eventId = 999L;
			EventUpdateRequest request = new EventUpdateRequest(
				"수정된 이벤트",
				EventCategory.POPUP,
				"수정된 설명",
				"수정된 장소",
				"https://example.com/updated-image.jpg",
				20000,
				80000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt,
				ticketCloseAt,
				200,
				EventStatus.PRE_OPEN
			);

			given(eventRepository.findById(eventId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> eventService.updateEvent(eventId, request))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.NOT_FOUND_EVENT);
		}
	}

	@Nested
	@DisplayName("이벤트 삭제")
	class DeleteEvent {

		@Test
		@DisplayName("이벤트 삭제 성공")
		void deleteEventSuccess() {
			// given
			Long eventId = 1L;
			Event existingEvent = createEvent(eventId, "테스트 이벤트", EventCategory.CONCERT, EventStatus.READY);

			given(eventRepository.findById(eventId)).willReturn(Optional.of(existingEvent));

			// when
			eventService.deleteEvent(eventId);

			// then
			assertThat(existingEvent.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 삭제 시 예외 발생")
		void deleteEventFailWhenEventNotFound() {
			// given
			Long eventId = 999L;
			given(eventRepository.findById(eventId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> eventService.deleteEvent(eventId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.NOT_FOUND_EVENT);
		}
	}

	@Nested
	@DisplayName("이벤트 단건 조회")
	class GetEvent {

		@Test
		@DisplayName("이벤트 단건 조회 성공")
		void getEventSuccess() {
			// given
			Long eventId = 1L;
			Event event = createEvent(eventId, "테스트 이벤트", EventCategory.CONCERT, EventStatus.READY);

			given(eventRepository.findById(eventId)).willReturn(Optional.of(event));

			// when
			EventResponse response = eventService.getEvent(eventId);

			// then
			assertThat(response.id()).isEqualTo(eventId);
			assertThat(response.title()).isEqualTo("테스트 이벤트");
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 조회 시 예외 발생")
		void getEventFailWhenEventNotFound() {
			// given
			Long eventId = 999L;
			given(eventRepository.findById(eventId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> eventService.getEvent(eventId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.NOT_FOUND_EVENT);
		}
	}

	@Nested
	@DisplayName("이벤트 목록 조회")
	class GetEvents {

		@Test
		@DisplayName("전체 이벤트 목록 조회 성공")
		void getEventsSuccess() {
			// given
			Pageable pageable = PageRequest.of(0, 10);
			List<Event> events = List.of(
				createEvent(1L, "이벤트1", EventCategory.CONCERT, EventStatus.READY),
				createEvent(2L, "이벤트2", EventCategory.POPUP, EventStatus.PRE_OPEN)
			);
			Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());

			given(eventRepository.findByConditions(null, null, pageable)).willReturn(eventPage);

			// when
			Page<EventListResponse> response = eventService.getEvents(null, null, pageable);

			// then
			assertThat(response.getContent()).hasSize(2);
			assertThat(response.getContent().get(0).title()).isEqualTo("이벤트1");
			assertThat(response.getContent().get(1).title()).isEqualTo("이벤트2");
		}

		@Test
		@DisplayName("상태별 이벤트 목록 조회 성공")
		void getEventsByStatusSuccess() {
			// given
			Pageable pageable = PageRequest.of(0, 10);
			EventStatus status = EventStatus.PRE_OPEN;
			List<Event> events = List.of(
				createEvent(1L, "이벤트1", EventCategory.CONCERT, EventStatus.PRE_OPEN)
			);
			Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());

			given(eventRepository.findByConditions(status, null, pageable)).willReturn(eventPage);

			// when
			Page<EventListResponse> response = eventService.getEvents(status, null, pageable);

			// then
			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().get(0).status()).isEqualTo(EventStatus.PRE_OPEN);
		}

		@Test
		@DisplayName("카테고리별 이벤트 목록 조회 성공")
		void getEventsByCategorySuccess() {
			// given
			Pageable pageable = PageRequest.of(0, 10);
			EventCategory category = EventCategory.CONCERT;
			List<Event> events = List.of(
				createEvent(1L, "콘서트 이벤트", EventCategory.CONCERT, EventStatus.READY)
			);
			Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());

			given(eventRepository.findByConditions(null, category, pageable)).willReturn(eventPage);

			// when
			Page<EventListResponse> response = eventService.getEvents(null, category, pageable);

			// then
			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().get(0).category()).isEqualTo(EventCategory.CONCERT);
		}

		@Test
		@DisplayName("상태와 카테고리 모두 필터링하여 이벤트 목록 조회 성공")
		void getEventsByStatusAndCategorySuccess() {
			// given
			Pageable pageable = PageRequest.of(0, 10);
			EventStatus status = EventStatus.PRE_OPEN;
			EventCategory category = EventCategory.POPUP;
			List<Event> events = List.of(
				createEvent(1L, "팝업 이벤트", EventCategory.POPUP, EventStatus.PRE_OPEN)
			);
			Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());

			given(eventRepository.findByConditions(status, category, pageable)).willReturn(eventPage);

			// when
			Page<EventListResponse> response = eventService.getEvents(status, category, pageable);

			// then
			assertThat(response.getContent()).hasSize(1);
			assertThat(response.getContent().get(0).status()).isEqualTo(EventStatus.PRE_OPEN);
			assertThat(response.getContent().get(0).category()).isEqualTo(EventCategory.POPUP);
		}
	}
}

