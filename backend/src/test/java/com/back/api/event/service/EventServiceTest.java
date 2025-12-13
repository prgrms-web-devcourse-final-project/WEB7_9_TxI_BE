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
		LocalDateTime rawNow = LocalDateTime.now();
		now = rawNow.withNano((rawNow.getNano() / 1000) * 1000);
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

		@Test
		@DisplayName("사전등록 시작일이 과거이면 예외 발생")
		void createEventFailWhenPreOpenAtIsBeforeNow() {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				now.minusDays(1), // 과거 날짜
				preCloseAt,
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
		@DisplayName("중복된 이벤트 생성 시 예외 발생")
		void createEventFailWhenDuplicateEvent() {
			// given
			// 먼저 이벤트 생성
			EventCreateRequest firstRequest = new EventCreateRequest(
				"중복 테스트 이벤트",
				EventCategory.CONCERT,
				"테스트 설명",
				"중복 테스트 장소",
				"https://example.com/image.jpg",
				10000,
				50000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt,
				ticketCloseAt,
				100
			);
			eventService.createEvent(firstRequest);

			// 동일한 title, place, ticketOpenAt로 두 번째 이벤트 생성 시도
			EventCreateRequest duplicateRequest = new EventCreateRequest(
				"중복 테스트 이벤트",
				EventCategory.CONCERT,
				"다른 설명",
				"중복 테스트 장소",
				"https://example.com/image2.jpg",
				20000,
				60000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt, // 동일한 ticketOpenAt
				ticketCloseAt,
				200
			);

			// when & then
			assertThatThrownBy(() -> eventService.createEvent(duplicateRequest))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.DUPLICATE_EVENT);
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

		@Test
		@DisplayName("다른 이벤트와 중복되는 정보로 수정 시 예외 발생")
		void updateEventFailWhenDuplicateWithOtherEvent() {
			// given
			// 첫 번째 이벤트 생성
			Event firstEvent = eventRepository.save(Event.builder()
				.title("첫 번째 이벤트")
				.category(EventCategory.CONCERT)
				.description("테스트 설명")
				.place("중복 테스트 장소")
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

			// 두 번째 이벤트 생성
			Event secondEvent = eventRepository.save(Event.builder()
				.title("두 번째 이벤트")
				.category(EventCategory.POPUP)
				.description("테스트 설명")
				.place("다른 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(ticketOpenAt.plusDays(1))
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build());

			// 두 번째 이벤트를 첫 번째 이벤트와 중복되는 정보로 수정 시도
			EventUpdateRequest request = new EventUpdateRequest(
				"첫 번째 이벤트", // 첫 번째 이벤트와 동일한 title
				EventCategory.POPUP,
				"수정된 설명",
				"중복 테스트 장소", // 첫 번째 이벤트와 동일한 place
				"https://example.com/updated-image.jpg",
				20000,
				80000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt, // 첫 번째 이벤트와 동일한 ticketOpenAt
				ticketCloseAt,
				200,
				EventStatus.PRE_OPEN
			);

			// when & then
			assertThatThrownBy(() -> eventService.updateEvent(secondEvent.getId(), request))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.DUPLICATE_EVENT);
		}

		@Test
		@DisplayName("자기 자신과 동일한 정보로 수정 시 성공")
		void updateEventSuccessWhenSameInfoForSelf() {
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

			// 동일한 title, place, ticketOpenAt으로 수정 (자기 자신이므로 성공해야 함)
			EventUpdateRequest request = new EventUpdateRequest(
				"기존 이벤트", // 동일한 title
				EventCategory.POPUP, // 다른 category
				"수정된 설명",
				"테스트 장소", // 동일한 place
				"https://example.com/updated-image.jpg",
				20000,
				80000,
				preOpenAt,
				preCloseAt,
				ticketOpenAt, // 동일한 ticketOpenAt
				ticketCloseAt,
				200,
				EventStatus.PRE_OPEN
			);

			// when
			EventResponse response = eventService.updateEvent(existingEvent.getId(), request);

			// then
			assertThat(response.title()).isEqualTo("기존 이벤트");
			assertThat(response.category()).isEqualTo(EventCategory.POPUP);
			assertThat(response.status()).isEqualTo(EventStatus.PRE_OPEN);
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
				event -> event.status() == EventStatus.PRE_OPEN
					&& event.category() == EventCategory.POPUP
			);
		}
	}

	@Nested
	@DisplayName("이벤트 엔티티 조회")
	class GetEventEntity {

		@Test
		@DisplayName("이벤트 엔티티 조회 성공")
		void getEventEntitySuccess() {
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
			Event result = eventService.getEventEntity(event.getId());

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isEqualTo(event.getId());
			assertThat(result.getTitle()).isEqualTo("테스트 이벤트");
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 엔티티 조회 시 예외 발생")
		void getEventEntityFailWhenEventNotFound() {
			// given
			Long eventId = 999L;

			// when & then
			assertThatThrownBy(() -> eventService.getEventEntity(eventId))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", EventErrorCode.NOT_FOUND_EVENT);
		}
	}

	@Nested
	@DisplayName("상태별 이벤트 조회")
	class FindEventsByStatus {

		@Test
		@DisplayName("특정 상태의 이벤트 목록 조회 성공")
		void findEventsByStatusSuccess() {
			// given
			eventRepository.save(Event.builder()
				.title("READY 이벤트1")
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
				.title("READY 이벤트2")
				.category(EventCategory.POPUP)
				.description("테스트 설명")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(ticketOpenAt.plusDays(1))
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build());

			eventRepository.save(Event.builder()
				.title("PRE_OPEN 이벤트")
				.category(EventCategory.DROP)
				.description("테스트 설명")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(ticketOpenAt.plusDays(2))
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.PRE_OPEN)
				.build());

			// when
			List<Event> readyEvents = eventService.findEventsByStatus(EventStatus.READY);

			// then
			assertThat(readyEvents).isNotEmpty();
			assertThat(readyEvents).allMatch(event -> event.getStatus() == EventStatus.READY);
			assertThat(readyEvents.size()).isGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("해당 상태의 이벤트가 없으면 빈 리스트 반환")
		void findEventsByStatusReturnEmptyListWhenNoEvents() {
			// when
			List<Event> events = eventService.findEventsByStatus(EventStatus.CLOSED);

			// then
			assertThat(events).isEmpty();
		}
	}

	@Nested
	@DisplayName("티켓 오픈 기간 및 상태별 이벤트 조회")
	class FindEventsByTicketOpenAtBetweenAndStatus {

		@Test
		@DisplayName("티켓 오픈 기간과 상태로 이벤트 조회 성공")
		void findEventsByTicketOpenAtBetweenAndStatusSuccess() {
			// given
			LocalDateTime searchStart = now.plusDays(5);
			LocalDateTime searchEnd = now.plusDays(15);

			// 검색 범위 내의 이벤트
			eventRepository.save(Event.builder()
				.title("범위 내 이벤트1")
				.category(EventCategory.CONCERT)
				.description("테스트 설명")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(now.plusDays(7)) // 범위 내
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build());

			eventRepository.save(Event.builder()
				.title("범위 내 이벤트2")
				.category(EventCategory.POPUP)
				.description("테스트 설명")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(now.plusDays(10)) // 범위 내
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build());

			// 검색 범위 밖의 이벤트
			eventRepository.save(Event.builder()
				.title("범위 밖 이벤트")
				.category(EventCategory.DROP)
				.description("테스트 설명")
				.place("테스트 장소")
				.imageUrl("https://example.com/image.jpg")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(preOpenAt)
				.preCloseAt(preCloseAt)
				.ticketOpenAt(now.plusDays(20)) // 범위 밖
				.ticketCloseAt(ticketCloseAt)
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build());

			// when
			List<Event> events = eventService.findEventsByTicketOpenAtBetweenAndStatus(
				searchStart, searchEnd, EventStatus.READY);

			// then
			assertThat(events).isNotEmpty();
			assertThat(events).allMatch(event ->
				event.getStatus() == EventStatus.READY
					&& !event.getTicketOpenAt().isBefore(searchStart)
					&& !event.getTicketOpenAt().isAfter(searchEnd)
			);
			assertThat(events.size()).isGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("기간 내에 해당하는 이벤트가 없으면 빈 리스트 반환")
		void findEventsByTicketOpenAtBetweenAndStatusReturnEmptyListWhenNoEvents() {
			// given
			LocalDateTime searchStart = now.plusYears(1);
			LocalDateTime searchEnd = now.plusYears(2);

			// when
			List<Event> events = eventService.findEventsByTicketOpenAtBetweenAndStatus(
				searchStart, searchEnd, EventStatus.READY);

			// then
			assertThat(events).isEmpty();
		}
	}
}
