package com.back.api.event.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.global.error.code.EventErrorCode;
import com.back.support.factory.EventFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminEventController 통합 테스트")
class AdminEventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EventRepository eventRepository;

	private LocalDateTime now;
	private LocalDateTime preOpenAt;
	private LocalDateTime preCloseAt;
	private LocalDateTime ticketOpenAt;
	private LocalDateTime ticketCloseAt;
	private LocalDateTime eventDate;

	@BeforeEach
	void setUp() {
		// 날짜 설정
		now = LocalDateTime.now().withNano(0);
		preOpenAt = now.plusDays(1);
		preCloseAt = now.plusDays(5);
		ticketOpenAt = now.plusDays(6);
		ticketCloseAt = now.plusDays(10);
		eventDate = now.plusDays(15);
	}

	@Nested
	@DisplayName("이벤트 생성 API (POST /api/v1/admin/events)")
	class CreateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 생성 성공 후 DB에 저장된다")
		void createEvent_Success() throws Exception {
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
				eventDate,
				100
			);

			// when & then
			mockMvc.perform(post("/api/v1/admin/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.title").value("테스트 이벤트"));

			// DB 확인
			List<Event> events = eventRepository.findAll();
			assertThat(events).hasSize(1);

			Event savedEvent = events.get(0);
			assertThat(savedEvent.getTitle()).isEqualTo("테스트 이벤트");
			assertThat(savedEvent.getCategory()).isEqualTo(EventCategory.CONCERT);
			assertThat(savedEvent.getMaxTicketAmount()).isEqualTo(100);
			assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.READY);
		}

		@Test
		@DisplayName("제목이 빈값이면 400 에러 발생")
		void createEvent_Fail_WhenTitleIsBlank() throws Exception {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"",
				EventCategory.CONCERT,
				"설명", "장소", "url", 1000, 2000,
				preOpenAt, preCloseAt, ticketOpenAt, ticketCloseAt, eventDate, 100
			);

			// when & then
			mockMvc.perform(post("/api/v1/admin/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("날짜 순서가 잘못되면 400 에러")
		void createEvent_Fail_InvalidDate() throws Exception {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"잘못된 날짜 이벤트",
				EventCategory.CONCERT, "설명", "장소", "url", 1000, 2000,
				preOpenAt, preCloseAt,
				ticketCloseAt.plusDays(1),
				ticketCloseAt,
				eventDate,
				100
			);

			// when & then
			mockMvc.perform(post("/api/v1/admin/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(EventErrorCode.INVALID_EVENT_DATE.getMessage()));
		}
	}

	@Nested
	@DisplayName("이벤트 수정 API (PUT /api/v1/admin/events/{eventId})")
	class UpdateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 수정 성공 및 DB 반영 확인")
		void updateEvent_Success() throws Exception {
			// given
			Event originalEvent = EventFactory.fakeEvent("기존 이벤트");
			eventRepository.save(originalEvent);

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
				now.plusDays(35),
				200,
				EventStatus.PRE_OPEN
			);

			// when & then
			mockMvc.perform(put("/api/v1/admin/events/{eventId}", originalEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("수정된 이벤트"))
				.andExpect(jsonPath("$.data.status").value("PRE_OPEN"));

			// DB 확인
			Event updatedEvent = eventRepository.findById(originalEvent.getId()).orElseThrow();
			assertThat(updatedEvent.getTitle()).isEqualTo("수정된 이벤트");
			assertThat(updatedEvent.getCategory()).isEqualTo(EventCategory.POPUP);
			assertThat(updatedEvent.getMaxTicketAmount()).isEqualTo(200);
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 수정 시 404 에러")
		void updateEvent_Fail_NotFound() throws Exception {
			// given
			EventUpdateRequest request = new EventUpdateRequest(
				"수정", EventCategory.CONCERT, "설명", "장소", "url",
				1000, 2000, preOpenAt, preCloseAt, ticketOpenAt, ticketCloseAt,
				now.plusDays(35), 100, EventStatus.READY
			);

			// when & then
			mockMvc.perform(put("/api/v1/admin/events/{eventId}", 999L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(EventErrorCode.NOT_FOUND_EVENT.getMessage()));
		}
	}

	@Nested
	@DisplayName("이벤트 삭제 API (DELETE /api/v1/admin/events/{eventId})")
	class DeleteEvent {

		@Test
		@DisplayName("존재하는 이벤트 삭제 성공 (soft delete)")
		void deleteEvent_Success() throws Exception {
			// given
			Event event = EventFactory.fakeEvent("삭제할 이벤트");
			Event savedEvent = eventRepository.save(event);
			Long eventId = savedEvent.getId();

			// when & then
			mockMvc.perform(delete("/api/v1/admin/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isNoContent());

			// DB 확인 - soft delete로 인해 findById는 empty를 반환해야 함
			assertThat(eventRepository.findById(eventId)).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 삭제 시 404 에러")
		void deleteEvent_Fail_NotFound() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/events/{eventId}", 999L))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(EventErrorCode.NOT_FOUND_EVENT.getMessage()));
		}
	}

	@Nested
	@DisplayName("전체 이벤트 대시보드 조회 API (GET /api/v1/admin/events/dashboard)")
	class GetAllEventsDashboard {

		@Test
		@DisplayName("모든 이벤트의 대시보드 정보를 조회한다")
		void getAllEventsDashboard_Success() throws Exception {
			// given
			Event event1 = EventFactory.fakeEvent("이벤트1");
			Event event2 = EventFactory.fakeEvent("이벤트2");
			eventRepository.saveAll(List.of(event1, event2));

			// when & then
			mockMvc.perform(get("/api/v1/admin/events/dashboard"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(2)))
				.andExpect(jsonPath("$.data[0].title", notNullValue()))
				.andExpect(jsonPath("$.data[0].status", notNullValue()))
				.andExpect(jsonPath("$.data[0].preRegisterCount", notNullValue()))
				.andExpect(jsonPath("$.data[0].totalSoldSeats", notNullValue()))
				.andExpect(jsonPath("$.data[0].totalSalesAmount", notNullValue()));
		}

		@Test
		@DisplayName("이벤트가 없으면 빈 배열을 반환한다")
		void getAllEventsDashboard_EmptyList() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/events/dashboard"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(0)));
		}
	}
}
