package com.back.api.event.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("EventService 통합 테스트")
class EventServiceTest {

	@Autowired
	private EventService eventService;

	@Autowired
	private EventRepository eventRepository;

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

			// when
			EventResponse response = eventService.createEvent(request);

			// then
			assertThat(response.id()).isNotNull();
			assertThat(response.title()).isEqualTo("테스트 이벤트");
			assertThat(response.category()).isEqualTo(EventCategory.CONCERT);
			assertThat(response.status()).isEqualTo(EventStatus.READY);

			// DB 검증
			Event savedEvent = eventRepository.findById(response.id()).orElseThrow();
			assertThat(savedEvent.getTitle()).isEqualTo("테스트 이벤트");
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
			Event existingEvent = eventRepository.save(Event.builder()
				.title("기존 이벤트")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.READY)
				.build());

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

			// when
			EventResponse response = eventService.updateEvent(existingEvent.getId(), request);

			// then
			assertThat(response.title()).isEqualTo("수정된 이벤트");
			assertThat(response.category()).isEqualTo(EventCategory.POPUP);
			assertThat(response.status()).isEqualTo(EventStatus.PRE_OPEN);

			// DB 검증
			Event updatedEvent = eventRepository.findById(existingEvent.getId()).orElseThrow();
			assertThat(updatedEvent.getTitle()).isEqualTo("수정된 이벤트");
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
			Event existingEvent = eventRepository.save(Event.builder()
				.title("테스트 이벤트")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.READY)
				.build());

			Long eventId = existingEvent.getId();

			// when
			eventService.deleteEvent(eventId);

			// then
			// 소프트 딜리트되어 findById로 조회되지 않음 (deleted = false 조건 때문)
			assertThat(eventRepository.findById(eventId)).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 삭제 시 예외 발생")
		void deleteEventFailWhenEventNotFound() {
			// given
			Long eventId = 999L;

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
			Event event = eventRepository.save(Event.builder()
				.title("테스트 이벤트")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.READY)
				.build());

			// when
			EventResponse response = eventService.getEvent(event.getId());

			// then
			assertThat(response.id()).isEqualTo(event.getId());
			assertThat(response.title()).isEqualTo("테스트 이벤트");
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 조회 시 예외 발생")
		void getEventFailWhenEventNotFound() {
			// given
			Long eventId = 999L;

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
			eventRepository.save(Event.builder()
				.title("이벤트1")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.READY)
				.build());

			eventRepository.save(Event.builder()
				.title("이벤트2")
				.category(EventCategory.POPUP)
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
				.status(EventStatus.PRE_OPEN)
				.build());

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<EventListResponse> response = eventService.getEvents(null, null, pageable);

			// then
			assertThat(response.getContent().size()).isGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("상태별 이벤트 목록 조회 성공")
		void getEventsByStatusSuccess() {
			// given
			eventRepository.save(Event.builder()
				.title("사전등록 이벤트")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.PRE_OPEN)
				.build());

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<EventListResponse> response = eventService.getEvents(EventStatus.PRE_OPEN, null, pageable);

			// then
			assertThat(response.getContent()).isNotEmpty();
			assertThat(response.getContent()).allMatch(event -> event.status() == EventStatus.PRE_OPEN);
		}

		@Test
		@DisplayName("카테고리별 이벤트 목록 조회 성공")
		void getEventsByCategorySuccess() {
			// given
			eventRepository.save(Event.builder()
				.title("콘서트 이벤트")
				.category(EventCategory.CONCERT)
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
				.status(EventStatus.READY)
				.build());

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<EventListResponse> response = eventService.getEvents(null, EventCategory.CONCERT, pageable);

			// then
			assertThat(response.getContent()).isNotEmpty();
			assertThat(response.getContent()).allMatch(event -> event.category() == EventCategory.CONCERT);
		}

		@Test
		@DisplayName("상태와 카테고리 모두 필터링하여 이벤트 목록 조회 성공")
		void getEventsByStatusAndCategorySuccess() {
			// given
			eventRepository.save(Event.builder()
				.title("팝업 이벤트")
				.category(EventCategory.POPUP)
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
				.status(EventStatus.PRE_OPEN)
				.build());

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<EventListResponse> response = eventService.getEvents(EventStatus.PRE_OPEN, EventCategory.POPUP,
				pageable);

			// then
			assertThat(response.getContent()).isNotEmpty();
			assertThat(response.getContent()).allMatch(
				event -> event.status() == EventStatus.PRE_OPEN && event.category() == EventCategory.POPUP);
		}
	}
}
