package com.back.domain.event.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

	@Autowired
	private EventRepository eventRepository;

	private LocalDateTime now;
	private LocalDateTime preOpenAt;
	private LocalDateTime preCloseAt;
	private LocalDateTime ticketOpenAt;
	private LocalDateTime ticketCloseAt;

	@BeforeEach
	void setUp() {
		eventRepository.deleteAll();
		now = LocalDateTime.now();
		preOpenAt = now.plusDays(1);
		preCloseAt = now.plusDays(5);
		ticketOpenAt = now.plusDays(6);
		ticketCloseAt = now.plusDays(10);
	}

	private Event createEvent(String title, EventCategory category, EventStatus status) {
		return Event.builder()
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
	}

	@Nested
	@DisplayName("조건별 이벤트 조회")
	class FindByConditions {

		@Test
		@DisplayName("모든 이벤트 조회")
		void findAllEvents() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(createEvent("이벤트2", EventCategory.POPUP, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트3", EventCategory.DROP, EventStatus.OPEN));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByConditions(null, null, pageable);

			// then
			assertThat(result.getContent()).hasSize(3);
		}

		@Test
		@DisplayName("상태별 이벤트 조회")
		void findByStatus() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트2", EventCategory.POPUP, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트3", EventCategory.DROP, EventStatus.READY));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByConditions(EventStatus.PRE_OPEN, null, pageable);

			// then
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent())
				.allMatch(event -> event.getStatus() == EventStatus.PRE_OPEN);
		}

		@Test
		@DisplayName("카테고리별 이벤트 조회")
		void findByCategory() {
			// given
			eventRepository.save(createEvent("콘서트1", EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(createEvent("콘서트2", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("팝업1", EventCategory.POPUP, EventStatus.READY));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByConditions(null, EventCategory.CONCERT, pageable);

			// then
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent())
				.allMatch(event -> event.getCategory() == EventCategory.CONCERT);
		}

		@Test
		@DisplayName("상태와 카테고리 모두 필터링하여 이벤트 조회")
		void findByStatusAndCategory() {
			// given
			eventRepository.save(createEvent("콘서트1", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("콘서트2", EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(createEvent("팝업1", EventCategory.POPUP, EventStatus.PRE_OPEN));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByConditions(
				EventStatus.PRE_OPEN, EventCategory.CONCERT, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getTitle()).isEqualTo("콘서트1");
		}

		@Test
		@DisplayName("페이징 처리 확인")
		void findWithPaging() {
			// given
			for (int i = 1; i <= 15; i++) {
				eventRepository.save(createEvent("이벤트" + i, EventCategory.CONCERT, EventStatus.READY));
			}

			Pageable firstPage = PageRequest.of(0, 10);
			Pageable secondPage = PageRequest.of(1, 10);

			// when
			Page<Event> firstResult = eventRepository.findByConditions(null, null, firstPage);
			Page<Event> secondResult = eventRepository.findByConditions(null, null, secondPage);

			// then
			assertThat(firstResult.getContent()).hasSize(10);
			assertThat(secondResult.getContent()).hasSize(5);
			assertThat(firstResult.getTotalElements()).isEqualTo(15);
			assertThat(firstResult.getTotalPages()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("상태별 이벤트 조회 (List)")
	class FindByStatus {

		@Test
		@DisplayName("특정 상태의 이벤트 목록 조회")
		void findByStatusReturnsList() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트2", EventCategory.POPUP, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트3", EventCategory.DROP, EventStatus.READY));

			// when
			List<Event> result = eventRepository.findByStatus(EventStatus.PRE_OPEN);

			// then
			assertThat(result).hasSize(2);
		}
	}

	@Nested
	@DisplayName("정렬 기능 테스트")
	class OrderBy {

		@Test
		@DisplayName("생성일 내림차순 정렬")
		void findAllByOrderByCreateAtDesc() {
			// given
			Event event1 = eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.READY));
			Event event2 = eventRepository.save(createEvent("이벤트2", EventCategory.POPUP, EventStatus.READY));
			Event event3 = eventRepository.save(createEvent("이벤트3", EventCategory.DROP, EventStatus.READY));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findAllByOrderByCreateAtDesc(pageable);

			// then
			assertThat(result.getContent()).hasSize(3);
		}

		@Test
		@DisplayName("상태별 생성일 내림차순 정렬")
		void findByStatusOrderByCreateAtDesc() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트2", EventCategory.POPUP, EventStatus.PRE_OPEN));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByStatusOrderByCreateAtDesc(
				EventStatus.PRE_OPEN, pageable);

			// then
			assertThat(result.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("카테고리별 생성일 내림차순 정렬")
		void findByCategoryOrderByCreateAtDesc() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(createEvent("이벤트2", EventCategory.CONCERT, EventStatus.PRE_OPEN));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByCategoryOrderByCreateAtDesc(
				EventCategory.CONCERT, pageable);

			// then
			assertThat(result.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("상태와 카테고리별 생성일 내림차순 정렬")
		void findByStatusAndCategoryOrderByCreateAtDesc() {
			// given
			eventRepository.save(createEvent("이벤트1", EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(createEvent("이벤트2", EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(createEvent("이벤트3", EventCategory.POPUP, EventStatus.PRE_OPEN));

			Pageable pageable = PageRequest.of(0, 10);

			// when
			Page<Event> result = eventRepository.findByStatusAndCategoryOrderByCreateAtDesc(
				EventStatus.PRE_OPEN, EventCategory.CONCERT, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getTitle()).isEqualTo("이벤트1");
		}
	}

	@Nested
	@DisplayName("기본 CRUD")
	class BasicCrud {

		@Test
		@DisplayName("이벤트 저장")
		void saveEvent() {
			// given
			Event event = createEvent("테스트 이벤트", EventCategory.CONCERT, EventStatus.READY);

			// when
			Event savedEvent = eventRepository.save(event);

			// then
			assertThat(savedEvent.getId()).isNotNull();
			assertThat(savedEvent.getTitle()).isEqualTo("테스트 이벤트");
		}

		@Test
		@DisplayName("이벤트 조회")
		void findEvent() {
			// given
			Event event = eventRepository.save(
				createEvent("테스트 이벤트", EventCategory.CONCERT, EventStatus.READY));

			// when
			Event foundEvent = eventRepository.findById(event.getId()).orElse(null);

			// then
			assertThat(foundEvent).isNotNull();
			assertThat(foundEvent.getTitle()).isEqualTo("테스트 이벤트");
		}

		@Test
		@DisplayName("이벤트 삭제")
		void deleteEvent() {
			// given
			Event event = eventRepository.save(
				createEvent("테스트 이벤트", EventCategory.CONCERT, EventStatus.READY));
			Long eventId = event.getId();

			// when
			eventRepository.delete(event);

			// then
			assertThat(eventRepository.findById(eventId)).isEmpty();
		}
	}
}

