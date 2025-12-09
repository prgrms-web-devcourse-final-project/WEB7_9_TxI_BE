package com.back.api.event.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.back.api.event.dto.request.EventCreateRequest;
import com.back.api.event.dto.request.EventUpdateRequest;
import com.back.api.event.dto.response.EventListResponse;
import com.back.api.event.dto.response.EventResponse;
import com.back.api.event.service.EventService;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.exception.ErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
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

	private EventResponse createEventResponse(Long id, String title) {
		return new EventResponse(
			id,
			title,
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
			100,
			EventStatus.READY
		);
	}

	@Nested
	@DisplayName("POST /api/v1/events - 이벤트 생성")
	class CreateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 생성 성공")
		void createEventSuccess() throws Exception {
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

			EventResponse response = createEventResponse(1L, "테스트 이벤트");
			given(eventService.createEvent(any(EventCreateRequest.class))).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.title").value("테스트 이벤트"))
				.andExpect(jsonPath("$.message").value("이벤트가 생성되었습니다."));
		}

		@Test
		@DisplayName("제목이 빈값이면 400 에러")
		void createEventFailWhenTitleIsBlank() throws Exception {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"",
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

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("카테고리가 null이면 400 에러")
		void createEventFailWhenCategoryIsNull() throws Exception {
			// given
			EventCreateRequest request = new EventCreateRequest(
				"테스트 이벤트",
				null,
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

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("최대 티켓 수량이 0이면 400 에러")
		void createEventFailWhenMaxTicketAmountIsZero() throws Exception {
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
				0
			);

			// when & then
			mockMvc.perform(post("/api/v1/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("PUT /api/v1/events/{eventId} - 이벤트 수정")
	class UpdateEvent {

		@Test
		@DisplayName("유효한 요청으로 이벤트 수정 성공")
		void updateEventSuccess() throws Exception {
			// given
			Long eventId = 1L;
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

			EventResponse response = new EventResponse(
				eventId,
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

			given(eventService.updateEvent(eq(eventId), any(EventUpdateRequest.class))).willReturn(response);

			// when & then
			mockMvc.perform(put("/api/v1/events/{eventId}", eventId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("수정된 이벤트"))
				.andExpect(jsonPath("$.message").value("이벤트가 수정되었습니다."));
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 수정 시 404 에러")
		void updateEventFailWhenEventNotFound() throws Exception {
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

			given(eventService.updateEvent(eq(eventId), any(EventUpdateRequest.class)))
				.willThrow(new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

			// when & then
			mockMvc.perform(put("/api/v1/events/{eventId}", eventId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("DELETE /api/v1/events/{eventId} - 이벤트 삭제")
	class DeleteEvent {

		@Test
		@DisplayName("이벤트 삭제 성공")
		void deleteEventSuccess() throws Exception {
			// given
			Long eventId = 1L;
			willDoNothing().given(eventService).deleteEvent(eventId);

			// when & then
			mockMvc.perform(delete("/api/v1/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.message").value("이벤트가 삭제되었습니다."));
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 삭제 시 404 에러")
		void deleteEventFailWhenEventNotFound() throws Exception {
			// given
			Long eventId = 999L;
			willThrow(new ErrorException(EventErrorCode.NOT_FOUND_EVENT))
				.given(eventService).deleteEvent(eventId);

			// when & then
			mockMvc.perform(delete("/api/v1/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("GET /api/v1/events/{eventId} - 이벤트 단건 조회")
	class GetEvent {

		@Test
		@DisplayName("이벤트 단건 조회 성공")
		void getEventSuccess() throws Exception {
			// given
			Long eventId = 1L;
			EventResponse response = createEventResponse(eventId, "테스트 이벤트");

			given(eventService.getEvent(eventId)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(eventId))
				.andExpect(jsonPath("$.data.title").value("테스트 이벤트"))
				.andExpect(jsonPath("$.message").value("이벤트를 조회했습니다."));
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 조회 시 404 에러")
		void getEventFailWhenEventNotFound() throws Exception {
			// given
			Long eventId = 999L;
			given(eventService.getEvent(eventId))
				.willThrow(new ErrorException(EventErrorCode.NOT_FOUND_EVENT));

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("GET /api/v1/events - 이벤트 목록 조회")
	class GetEvents {

		@Test
		@DisplayName("전체 이벤트 목록 조회 성공")
		void getEventsSuccess() throws Exception {
			// given
			List<EventListResponse> events = List.of(
				new EventListResponse(1L, "이벤트1", EventCategory.CONCERT, "장소1",
					"https://example.com/image1.jpg", 10000, 50000,
					preOpenAt, preCloseAt, ticketOpenAt, EventStatus.READY, now),
				new EventListResponse(2L, "이벤트2", EventCategory.POPUP, "장소2",
					"https://example.com/image2.jpg", 20000, 80000,
					preOpenAt, preCloseAt, ticketOpenAt, EventStatus.PRE_OPEN, now)
			);
			Page<EventListResponse> eventPage = new PageImpl<>(events, PageRequest.of(0, 10), events.size());

			given(eventService.getEvents(any(), any(), any())).willReturn(eventPage);

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content.length()").value(2))
				.andExpect(jsonPath("$.message").value("이벤트 목록을 조회했습니다."));
		}

		@Test
		@DisplayName("상태별 이벤트 목록 조회 성공")
		void getEventsByStatusSuccess() throws Exception {
			// given
			List<EventListResponse> events = List.of(
				new EventListResponse(1L, "이벤트1", EventCategory.CONCERT, "장소1",
					"https://example.com/image.jpg", 10000, 50000,
					preOpenAt, preCloseAt, ticketOpenAt, EventStatus.PRE_OPEN, now)
			);
			Page<EventListResponse> eventPage = new PageImpl<>(events, PageRequest.of(0, 10), events.size());

			given(eventService.getEvents(any(), any(), any())).willReturn(eventPage);

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("status", "PRE_OPEN")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content.length()").value(1));
		}

		@Test
		@DisplayName("카테고리별 이벤트 목록 조회 성공")
		void getEventsByCategorySuccess() throws Exception {
			// given
			List<EventListResponse> events = List.of(
				new EventListResponse(1L, "콘서트 이벤트", EventCategory.CONCERT, "장소1",
					"https://example.com/image.jpg", 10000, 50000,
					preOpenAt, preCloseAt, ticketOpenAt, EventStatus.READY, now)
			);
			Page<EventListResponse> eventPage = new PageImpl<>(events, PageRequest.of(0, 10), events.size());

			given(eventService.getEvents(any(), any(), any())).willReturn(eventPage);

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("category", "CONCERT")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content.length()").value(1));
		}
	}
}
