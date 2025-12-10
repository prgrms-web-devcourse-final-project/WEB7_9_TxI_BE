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
@DisplayName("EventController 통합 테스트")
class EventControllerTest {

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

	@BeforeEach
	void setUp() {
		// 날짜 설정
		now = LocalDateTime.now().withNano(0);
		preOpenAt = now.plusDays(1);
		preCloseAt = now.plusDays(5);
		ticketOpenAt = now.plusDays(6);
		ticketCloseAt = now.plusDays(10);
	}

	@Nested
	@DisplayName("이벤트 생성 API (POST /api/v1/events)")
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
				100
			);

			// when
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("이벤트가 생성되었습니다."))
				.andExpect(jsonPath("$.data.title").value("테스트 이벤트"));

			// then
			List<Event> events = eventRepository.findAll();
			assertThat(events).hasSize(1);

			Event savedEvent = events.get(0);
			assertThat(savedEvent.getTitle()).isEqualTo("테스트 이벤트");
			assertThat(savedEvent.getCategory()).isEqualTo(EventCategory.CONCERT);
			assertThat(savedEvent.getMaxTicketAmount()).isEqualTo(100);
			assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.READY); // 기본값 확인
		}

		@Test
		@DisplayName("제목이 빈값이면 400 에러 발생")
		void createEvent_Fail_WhenTitleIsBlank() throws Exception {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"", // 제목 빈값
				EventCategory.CONCERT,
				"설명", "장소", "url", 1000, 2000,
				preOpenAt, preCloseAt, ticketOpenAt, ticketCloseAt, 100
			);

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("날짜 순서가 잘못되면 400 에러 (검증 로직 동작 확인)")
		void createEvent_Fail_InvalidDate() throws Exception {
			// given: 티켓 오픈일이 마감일보다 늦음
			EventCreateRequest request = new EventCreateRequest(
				"잘못된 날짜 이벤트",
				EventCategory.CONCERT, "설명", "장소", "url", 1000, 2000,
				preOpenAt, preCloseAt,
				ticketCloseAt.plusDays(1), // ticketOpenAt이 CloseAt보다 늦음
				ticketCloseAt,
				100
			);

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(EventErrorCode.INVALID_EVENT_DATE.getMessage()));
		}
	}

	@Nested
	@DisplayName("이벤트 수정 API (PUT /api/v1/events/{eventId})")
	class UpdateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 수정 성공 및 DB 반영 확인")
		void updateEvent_Success() throws Exception {
			// given: 기존 이벤트 저장
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
				200,
				EventStatus.PRE_OPEN
			);

			// when
			mockMvc.perform(put("/api/v1/events/{eventId}", originalEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("이벤트가 수정되었습니다."))
				.andExpect(jsonPath("$.data.title").value("수정된 이벤트"))
				.andExpect(jsonPath("$.data.status").value("PRE_OPEN"));

			// then: DB 변경 확인
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
				"수정", EventCategory.POPUP, "설명", "장소", "url",
				1000, 2000, preOpenAt, preCloseAt, ticketOpenAt, ticketCloseAt, 100, EventStatus.PRE_OPEN
			);

			// when & then
			mockMvc.perform(put("/api/v1/events/{eventId}", 99999L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(EventErrorCode.NOT_FOUND_EVENT.getMessage()));
		}
	}

	@Nested
	@DisplayName("이벤트 삭제 API (DELETE /api/v1/events/{eventId})")
	class DeleteEvent {

		@Test
		@DisplayName("이벤트 삭제 성공 시 DB에서 조회되지 않아야 한다 (Soft Delete)")
		void deleteEvent_Success() throws Exception {
			// given
			Event event = eventRepository.save(EventFactory.fakeEvent());

			// when
			mockMvc.perform(delete("/api/v1/events/{eventId}", event.getId()))
				.andDo(print())
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.message").value("이벤트가 삭제되었습니다."));

			// then: Repository의 findById는 deleted=false만 조회하므로 empty여야 함
			assertThat(eventRepository.findById(event.getId())).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 삭제 시 404 에러")
		void deleteEvent_Fail_NotFound() throws Exception {
			mockMvc.perform(delete("/api/v1/events/{eventId}", 99999L))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("이벤트 단건 조회 API (GET /api/v1/events/{eventId})")
	class GetEvent {

		@Test
		@DisplayName("이벤트 단건 조회 성공")
		void getEvent_Success() throws Exception {
			// given
			Event event = eventRepository.save(EventFactory.fakeEvent("단건 조회 이벤트"));

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", event.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(event.getId()))
				.andExpect(jsonPath("$.data.title").value("단건 조회 이벤트"));
		}
	}

	@Nested
	@DisplayName("이벤트 목록 조회 API (GET /api/v1/events)")
	class GetEvents {

		@Test
		@DisplayName("전체 이벤트 목록 조회 성공")
		void getEvents_Success() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent("이벤트1"));
			eventRepository.save(EventFactory.fakeEvent("이벤트2"));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(2)));
		}

		@Test
		@DisplayName("카테고리별 조회 성공")
		void getEvents_ByCategory_Success() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent(EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(EventFactory.fakeEvent(EventCategory.POPUP, EventStatus.READY));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("category", "CONCERT")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(1)))
				.andExpect(jsonPath("$.data.content[0].category").value("CONCERT"));
		}

		@Test
		@DisplayName("상태별 조회 성공")
		void getEvents_ByStatus_Success() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent(EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(EventFactory.fakeEvent(EventCategory.CONCERT, EventStatus.CLOSED));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("status", "PRE_OPEN")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(1)))
				.andExpect(jsonPath("$.data.content[0].status").value("PRE_OPEN"));
		}
	}
}

