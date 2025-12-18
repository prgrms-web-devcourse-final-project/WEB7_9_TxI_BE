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
import org.springframework.http.MediaType;
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

	@Nested
	@DisplayName("상위 N명 입장 처리 API (/api/v1/queues/{eventId}/process-entries")
	class ProcessTopEntriesTest  {
		@Test
		@DisplayName("상위 1명 입장 처리 - 기본값")
		void processTopEntries_Default_Success() throws Exception {
			// given
			for (int i = 1; i <= 5; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-entries", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("입장 처리가 완료되었습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.processedCount").value(1))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(4))
				.andDo(print());
		}

		@Test
		@DisplayName("상위 5명 입장 처리")
		void processTopEntries_Count5_Success() throws Exception {
			// given
			for (int i = 1; i <= 10; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-entries", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"count\": 5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.processedCount").value(5))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(5))
				.andDo(print());
		}

		@Test
		@DisplayName("대기 인원보다 많은 수 요청")
		void processTopEntries_CountExceedsWaiting() throws Exception {
			// given
			for (int i = 1; i <= 3; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-entries", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"count\": 10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.processedCount").value(3))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(0))
				.andDo(print());
		}

		@Test
		@DisplayName("대기 인원이 없을 때 - 0명 처리")
		void processTopEntries_NoWaiting_ProcessZero() throws Exception {

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-entries", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"count\": 5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.processedCount").value(0))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(0))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("내 앞 사용자 모두 입장 처리 API (/api/v1/queues/{eventId}/process-until-me")
	class ProcessUntilMeTest  {
		@Test
		@DisplayName("70번째 사용자 - 앞의 69명 입장 처리")
		void processUntilMe_Rank77_Process76Users() throws Exception {
			// given
			for (int i = 1; i <= 69; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}


			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 70);

			for (int i = 71; i <= 100; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("내 앞 사용들이 모두 입장 처리가 완료되었습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.processedCount").value(69))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(31))
				.andDo(print());
		}

		@Test
		@DisplayName("1순위 사용자 - 처리할 사용자 없음")
		void processUntilMe_Rank1_ProcessZero() throws Exception {
			// given
			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);


			for (int i = 2; i <= 10; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.processedCount").value(0))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(10))
				.andDo(print());
		}

		@Test
		@DisplayName("5순위 사용자 - 앞의 4명 입장 처리")
		void processUntilMe_Rank5_Process4Users() throws Exception {
			// given
			for (int i = 1; i <= 4; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 5);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.processedCount").value(4))
				.andExpect(jsonPath("$.data.remainingWaitingCount").value(1))  // 나만 남음
				.andDo(print());
		}

		@Test
		@DisplayName("대기열에 없는 사용자 - 실패")
		void processUntilMe_NotInQueue_Fail() throws Exception {
			// given

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("큐 대기열 항목을 찾을 수 없습니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("이미 입장한 사용자 - 실패")
		void processUntilMe_AlreadyEntered_Fail() throws Exception {
			// given
			queueEntryHelper.createEnteredQueueEntryWithRedis(testEvent, testUser);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("대기중 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("만료된 사용자 - 실패")
		void processUntilMe_Expired_Fail() throws Exception {
			// given
			queueEntryHelper.createExpiredQueueEntry(testEvent, testUser);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("대기중 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("결제 완료된 사용자 - 실패")
		void processUntilMe_Completed_Fail() throws Exception {
			// given
			queueEntryHelper.createCompletedQueueEntry(testEvent, testUser);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/process-until-me", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("대기중 상태가 아닙니다."))
				.andDo(print());
		}
	}
}
