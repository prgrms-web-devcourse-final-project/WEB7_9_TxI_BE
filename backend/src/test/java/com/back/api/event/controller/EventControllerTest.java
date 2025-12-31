package com.back.api.event.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.s3.service.S3PresignedService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.UserRole;
import com.back.support.factory.EventFactory;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TestAuthHelper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("EventController 통합 테스트")
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventRepository eventRepository;

	@MockitoBean
	private S3PresignedService s3PresignedService;

	@Autowired
	private TestAuthHelper authHelper;

	@Autowired
	private StoreHelper storeHelper;

	String token;

	Store store;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();
		when(s3PresignedService.issueDownloadUrl(anyString()))
			.thenReturn("https://mocked-presigned-url");

		token = authHelper.issueAccessToken(UserRole.NORMAL, null);
	}

	@Nested
	@DisplayName("이벤트 단건 조회 API (GET /api/v1/events/{eventId})")
	class GetEvent {

		@Test
		@DisplayName("이벤트 단건 조회 성공")
		void getEvent_Success() throws Exception {
			// given
			Event event = eventRepository.save(EventFactory.fakeEvent(store, "단건 조회 이벤트"));

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", event.getId())
					.header("Authorization", "Bearer " + token))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(event.getId()))
				.andExpect(jsonPath("$.data.title").value("단건 조회 이벤트"));
		}

		@Test
		@DisplayName("비회원도 이벤트 단건 조회 가능")
		void getEvent_Success_WithoutAuth() throws Exception {
			// given
			Event event = eventRepository.save(EventFactory.fakeEvent(store, "비회원 조회 이벤트"));

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", event.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(event.getId()))
				.andExpect(jsonPath("$.data.title").value("비회원 조회 이벤트"));
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 조회 시 404 에러")
		void getEvent_Fail_NotFound() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}", 999L)
					.header("Authorization", "Bearer " + token))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("이벤트 목록 조회 API (GET /api/v1/events)")
	class GetEvents {

		@Test
		@DisplayName("전체 이벤트 목록 조회 성공")
		void getEvents_Success() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent(store, "이벤트1"));
			eventRepository.save(EventFactory.fakeEvent(store, "이벤트2"));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.header("Authorization", "Bearer " + token)
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(2)));
		}

		@Test
		@DisplayName("비회원도 이벤트 목록 조회 가능")
		void getEvents_Success_WithoutAuth() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent(store, "비회원 이벤트1"));
			eventRepository.save(EventFactory.fakeEvent(store, "비회원 이벤트2"));
			eventRepository.save(EventFactory.fakeEvent(store, "비회원 이벤트3"));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.param("page", "0")
					.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(3))));
		}

		@Test
		@DisplayName("카테고리별 조회 성공")
		void getEvents_ByCategory_Success() throws Exception {
			// given
			eventRepository.save(EventFactory.fakeEvent(store, EventCategory.CONCERT, EventStatus.READY));
			eventRepository.save(EventFactory.fakeEvent(store, EventCategory.POPUP, EventStatus.READY));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.header("Authorization", "Bearer " + token)
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
			eventRepository.save(EventFactory.fakeEvent(store, EventCategory.CONCERT, EventStatus.PRE_OPEN));
			eventRepository.save(EventFactory.fakeEvent(store, EventCategory.CONCERT, EventStatus.CLOSED));

			// when & then
			mockMvc.perform(get("/api/v1/events")
					.header("Authorization", "Bearer " + token)
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
