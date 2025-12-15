package com.back.api.queue.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.factory.UserFactory;
import com.back.support.helper.EventHelper;
import com.back.support.helper.QueueEntryHelper;
import com.back.support.helper.TestAuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
@DisplayName("AdminQueueEntryController 통합 테스트")
public class AdminQueueEntryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private QueueEntryHelper queueEntryHelper;

	@Autowired
	private TestAuthHelper authHelper;

	private Event testEvent;
	private User adminUser;

	@BeforeEach
	void setUp() {
		testEvent = eventHelper.createEvent("TestEvent");

		adminUser = UserFactory.fakeUser(UserRole.ADMIN, passwordEncoder).user();
		userRepository.save(adminUser);

		authHelper.authenticate(adminUser);
		queueEntryHelper.clearRedis(testEvent.getId());
	}

	@Nested
	@DisplayName("대기열 랜덤 섞기 API (/api/v1/admin/queues/{eventId}/shuffle)")
	class ShuffleQueueTest {

		@Test
		@DisplayName("사전 등록 사용자 목록으로 랜덤 큐 생성")
		void shuffleQueue_Success() throws Exception {

			//given
			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user1);
			userRepository.save(user2);
			userRepository.save(user3);

			List<Long> userIds = List.of(user1.getId(), user2.getId(), user3.getId());
			String requestBody = objectMapper.writeValueAsString(
				new ShuffleQueueRequest(userIds)
			);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/shuffle", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("랜덤 큐가 생성되었습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.totalCount").value(3))
				.andDo(print());
		}

		@Test
		@DisplayName("빈 사전 등록 인원으로 랜덤 큐 생성")
		void shuffleQueue_EmptyList_Fail() throws Exception {
			// given
			String requestBody = objectMapper.writeValueAsString(
				new ShuffleQueueRequest(List.of())
			);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/shuffle", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andDo(print());
		}

		@Test
		@DisplayName("이미 존재하는 대기열")
		void shuffleQueue_AlreadyExists_Fail() throws Exception {
			// given
			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user1);
			queueEntryHelper.createQueueEntry(testEvent, user1, 1);

			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user2);
			List<Long> userIds = List.of(user2.getId());
			String requestBody = objectMapper.writeValueAsString(
				new ShuffleQueueRequest(userIds)
			);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/shuffle", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이미 대기열이 존재합니다."))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("대기열 통계 조회 API (/api/v1/admin/queues/{eventId}/statistics)")
	class GetQueueStatisticsTests {

		@Test
		@DisplayName("대기열 통계 조회")
		void getQueueStatistics_Success() throws Exception {

			// given
			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user4 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user1);
			userRepository.save(user2);
			userRepository.save(user3);
			userRepository.save(user4);

			queueEntryHelper.createQueueEntry(testEvent, user1, 1, QueueEntryStatus.WAITING);
			queueEntryHelper.createQueueEntry(testEvent, user2, 2, QueueEntryStatus.ENTERED);
			queueEntryHelper.createQueueEntry(testEvent, user3, 3, QueueEntryStatus.EXPIRED);
			queueEntryHelper.createQueueEntry(testEvent, user4, 4, QueueEntryStatus.COMPLETED);

			// when then
			mockMvc.perform(get("/api/v1/admin/queues/{eventId}/statistics", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 통계를 조회했습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.totalCount").value(4))
				.andExpect(jsonPath("$.data.waitingCount").value(1))
				.andExpect(jsonPath("$.data.enteredCount").value(1))
				.andExpect(jsonPath("$.data.expiredCount").value(1))
				.andDo(print());
		}

		@Test
		@DisplayName("대기열이 없는 경우")
		void getQueueStatistics_NotFound_Fail() throws Exception {

			// when then
			mockMvc.perform(get("/api/v1/admin/queues/{eventId}/statistics", testEvent.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("큐 대기열 항목을 찾을 수 없습니다."))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("결제 완료 처리 API (/api/v1/admin/queues/{eventId}/users/{userId}/complete)")
	class CompletePaymentTests {

		@Test
		@DisplayName("입장한 사용자 결제 완료 처리")
		void completePayment_Success() throws Exception {

			// given
			User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user);
			queueEntryHelper.createEnteredQueueEntryWithRedis(testEvent, user);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/users/{userId}/complete",
					testEvent.getId(), user.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("결제 완료 처리되었습니다."))
				.andExpect(jsonPath("$.data.userId").value(user.getId()))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.status").value("COMPLETED"))
				.andDo(print());
		}

		@Test
		@DisplayName("대기 중인 사용자는 결제 완료 처리 실패")
		void completePayment_Waiting_Fail() throws Exception {
			// given
			User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user);
			queueEntryHelper.createQueueEntry(testEvent, user, 1);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/users/{userId}/complete",
					testEvent.getId(), user.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("입장 완료 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("이미 결제가 된 사용자")
		void completePayment_AlreadyCompleted_Fail() throws Exception {
			// given
			User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user);
			queueEntryHelper.createCompletedQueueEntry(testEvent, user);

			// when then
			mockMvc.perform(post("/api/v1/admin/queues/{eventId}/users/{userId}/complete",
					testEvent.getId(), user.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이미 결제가 완료되었습니다."))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("대기열 초기화 API (/api/v1/admin/queues/{eventId}/reset)")
	class ResetQueueTests {

		@Test
		@DisplayName("대기열 초기화")
		void resetQueue_Success() throws Exception {
			// GIVEN: 대기열 데이터 생성
			User user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			User user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder).user();
			userRepository.save(user1);
			userRepository.save(user2);

			queueEntryHelper.createQueueEntryWithRedis(testEvent, user1, 1);
			queueEntryHelper.createQueueEntryWithRedis(testEvent, user2, 2);

			// when then
			mockMvc.perform(delete("/api/v1/admin/queues/{eventId}/reset", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열이 초기화되었습니다."))
				.andDo(print());
		}

	}

	// DTO record for request body
	record ShuffleQueueRequest(List<Long> preRegisteredUserIds) { }

}
