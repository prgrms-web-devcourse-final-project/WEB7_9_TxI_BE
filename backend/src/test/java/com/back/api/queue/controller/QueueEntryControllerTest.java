package com.back.api.queue.controller;

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
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.config.TestRedisConfig;
import com.back.domain.event.entity.Event;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.factory.UserFactory;
import com.back.support.helper.EventHelper;
import com.back.support.helper.QueueEntryHelper;
import com.back.support.helper.TestAuthHelper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
@DisplayName("QueueEntryController 통합 테스트")
public class QueueEntryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private QueueEntryHelper queueEntryHelper;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private TestAuthHelper testAuthHelper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User testUser;
	private Event testEvent;

	@BeforeEach
	void setUp() {

		testEvent = eventHelper.createEvent("TestEvent");

		// 테스트 유저 생성 & 저장
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
		userRepository.save(testUser);

		// 인증 흐름 통과시키기
		testAuthHelper.authenticate(testUser);

		queueEntryRedisRepository.clearAll(testEvent.getId());
	}

	@Nested
	@DisplayName("대기열 상태 조회 API (/api/v1/queues/{eventId}/status")
	class GetQueueStatusTest {

		@Test
		@DisplayName("WAITING 상태의 사용자 조회")
		void getWaitingQueue_Success() throws Exception {
			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);

			mockMvc.perform(get("/api/v1/queues/{eventId}/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("WAITING"))
				.andExpect(jsonPath("$.data.queueRank").value(1))
				.andDo(print());

		}

		@Test
		@DisplayName("ENTERED 상태의 사용자 조회")
		void getEnteredQueue_Success() throws Exception {
			queueEntryHelper.createEnteredQueueEntryWithRedis(testEvent, testUser);

			mockMvc.perform(get("/api/v1/queues/{eventId}/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ENTERED"))
				.andDo(print());

		}

		@Test
		@DisplayName("COMPLETED 상태의 사용자 조회")
		void getCompletedQueue_Success() throws Exception {
			queueEntryHelper.createCompletedQueueEntry(testEvent, testUser);

			mockMvc.perform(get("/api/v1/queues/{eventId}/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("COMPLETED"))
				.andDo(print());

		}

		@Test
		@DisplayName("EXPIRED 상태의 사용자 조회")
		void getExpiredQueue_Success() throws Exception {
			queueEntryHelper.createExpiredQueueEntry(testEvent, testUser);

			mockMvc.perform(get("/api/v1/queues/{eventId}/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("EXPIRED"))
				.andDo(print());

		}

		@Test
		@DisplayName("대기열에 없는 사용자의 대기열 상태 조회")
		void getNotInQueueUser_Fail() throws Exception {
			mockMvc.perform(get("/api/v1/queues/{eventId}/status", testEvent.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("큐 대기열 항목을 찾을 수 없습니다."))
				.andDo(print());

		}

	}

	@Nested
	@DisplayName("대기열 진입 여부 조회 API (/api/v1/queues/{eventId}/exists")
	class GetQueueExistsTest {
		@Test
		@DisplayName("대기 중인 사용자 조회")
		void existsInQueue_Waiting_ReturnsTrue() throws Exception {
			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 3);

			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(true))
				.andDo(print());
		}

		@Test
		@DisplayName("대기열에 없는 사용자 조회")
		void existsInQueue_NotInQueue_ReturnsFalse() throws Exception {
			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(false))
				.andDo(print());
		}
	}
}
