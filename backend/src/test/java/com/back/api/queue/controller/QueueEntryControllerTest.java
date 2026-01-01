package com.back.api.queue.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
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
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.factory.UserFactory;
import com.back.support.helper.EventHelper;
import com.back.support.helper.QueueEntryHelper;
import com.back.support.helper.StoreHelper;
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
	private QueueEntryRepository queueEntryRepository;

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

	@Autowired
	private StoreHelper storeHelper;

	private User testUser;
	private Event testEvent;
	private Store store;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();
		testEvent = eventHelper.createEvent(store, "TestEvent");

		// 테스트 유저 생성 & 저장
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
	@DisplayName("대기열 입장 완료 여부 조회 API (/api/v1/queues/{eventId}/exists")
	class GetQueueExistsTest {
		@Test
		@DisplayName("입장 완료한 사용자 조회 - true 반환")
		void existsInQueue_Entered_ReturnsTrue() throws Exception {
			// ENTERED 상태로 생성
			QueueEntry queueEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);
			queueEntry.enterQueue(); // ENTERED 상태로 변경
			queueEntryRepository.save(queueEntry);
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), testUser.getId());

			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 입장 완료 여부를 확인했습니다.(ENTERED)"))
				.andExpect(jsonPath("$.data").value(true))
				.andDo(print());
		}

		@Test
		@DisplayName("대기 중인 사용자 조회 - false 반환")
		void existsInQueue_Waiting_ReturnsFalse() throws Exception {
			// WAITING 상태로만 생성
			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 3);

			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 입장 완료 여부를 확인했습니다.(ENTERED)"))
				.andExpect(jsonPath("$.data").value(false))
				.andDo(print());
		}

		@Test
		@DisplayName("대기열에 없는 사용자 조회 - false 반환")
		void existsInQueue_NotInQueue_ReturnsFalse() throws Exception {
			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 입장 완료 여부를 확인했습니다.(ENTERED)"))
				.andExpect(jsonPath("$.data").value(false))
				.andDo(print());
		}

		@Test
		@DisplayName("만료된 사용자 조회 - false 반환")
		void existsInQueue_Expired_ReturnsFalse() throws Exception {
			QueueEntry queueEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);
			queueEntry.enterQueue();
			queueEntry.expire(); // EXPIRED 상태로 변경
			queueEntryRepository.save(queueEntry);

			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 입장 완료 여부를 확인했습니다.(ENTERED)"))
				.andExpect(jsonPath("$.data").value(false))
				.andDo(print());
		}

		@Test
		@DisplayName("결제 완료한 사용자 조회 - false 반환")
		void existsInQueue_Completed_ReturnsFalse() throws Exception {
			QueueEntry queueEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);
			queueEntry.enterQueue();
			queueEntry.completePayment(); // COMPLETED 상태로 변경
			queueEntryRepository.save(queueEntry);

			mockMvc.perform(get("/api/v1/queues/{eventId}/exists", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 입장 완료 여부를 확인했습니다.(ENTERED)"))
				.andExpect(jsonPath("$.data").value(false))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("상위 N명 입장 처리 API (/api/v1/queues/{eventId}/process-entries")
	class ProcessTopEntriesTest {
		@Test
		@DisplayName("상위 1명 입장 처리 - 기본값")
		void processTopEntries_Default_Success() throws Exception {
			// given
			for (int i = 1; i <= 5; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
	class ProcessUntilMeTest {
		@Test
		@DisplayName("70번째 사용자 - 앞의 69명 입장 처리")
		void processUntilMe_Rank77_Process76Users() throws Exception {
			// given
			for (int i = 1; i <= 69; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 70);

			for (int i = 71; i <= 100; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
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

	@Nested
	@DisplayName("사용자 대기열 맨 뒤로 이동 API (/api/v1/queues/{eventId}/move-to-back)")
	class MoveToBackTest {

		@Test
		@DisplayName("입장 완료 상태에서 맨 뒤로 이동 - 성공")
		void moveToBack_Success() throws Exception {

			// given
			for (int i = 1; i <= 3; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			QueueEntry enteredEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 10);
			enteredEntry.enterQueue();
			queueEntryRepository.save(enteredEntry);
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), testUser.getId());

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("대기열 맨 뒤로 이동되었습니다."))
				.andExpect(jsonPath("$.data.userId").value(testUser.getId()))
				.andExpect(jsonPath("$.data.previousRank").value(10))
				.andExpect(jsonPath("$.data.newRank").value(11))
				.andExpect(jsonPath("$.data.totalWaitingUsers").value(4))
				.andDo(print());

			// DB 검증
			QueueEntry updatedEntry = queueEntryRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.getId())
				.orElseThrow();
			assertThat(updatedEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.WAITING);
			assertThat(updatedEntry.getQueueRank()).isEqualTo(11);
		}

		@Test
		@DisplayName("여러 ENTERED 사용자 있을 때 - 전체 최대 rank 기준")
		void moveToBack_WithMultipleEnteredUsers() throws Exception {

			// given
			for (int i = 1; i <= 3; i++) {
				User user = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
				userRepository.save(user);
				queueEntryHelper.createQueueEntryWithRedis(testEvent, user, i);
			}

			User user4 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null).user();
			userRepository.save(user4);
			QueueEntry entered1 = queueEntryHelper.createQueueEntryWithRedis(testEvent, user4, 100);
			entered1.enterQueue();
			queueEntryRepository.save(entered1);

			QueueEntry entered2 = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 150);
			entered2.enterQueue();
			queueEntryRepository.save(entered2);
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), testUser.getId());

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.previousRank").value(150))
				.andExpect(jsonPath("$.data.newRank").value(151))
				.andExpect(jsonPath("$.data.totalWaitingUsers").value(4))
				.andDo(print());
		}

		@Test
		@DisplayName("대기자가 없을 때 - 맨 뒤로 이동")
		void moveToBack_NoWaitingUsers() throws Exception {

			// given
			QueueEntry enteredEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 50);
			enteredEntry.enterQueue();
			queueEntryRepository.save(enteredEntry);
			queueEntryRedisRepository.moveToEnteredQueue(testEvent.getId(), testUser.getId());

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.previousRank").value(50))
				.andExpect(jsonPath("$.data.newRank").value(51))
				.andExpect(jsonPath("$.data.totalWaitingUsers").value(1))
				.andDo(print());
		}

		@Test
		@DisplayName("WAITING 상태에서 시도")
		void moveToBack_NotEnteredStatus() throws Exception {

			// given
			queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("입장 완료 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("EXPIRED 상태에서 시도")
		void moveToBack_ExpiredStatus() throws Exception {

			// given
			QueueEntry expiredEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);
			expiredEntry.enterQueue();
			expiredEntry.expire();
			queueEntryRepository.save(expiredEntry);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("입장 완료 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("COMPLETED 상태에서 시도")
		void moveToBack_CompletedStatus() throws Exception {

			// given
			QueueEntry completedEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 1);
			completedEntry.enterQueue();
			completedEntry.completePayment();
			queueEntryRepository.save(completedEntry);

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("입장 완료 상태가 아닙니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("QueueEntry 없음")
		void moveToBack_NotFound() throws Exception {

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("큐 대기열 항목을 찾을 수 없습니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("Redis 없어도 DB는 정상 업데이트")
		void moveToBack_WithoutRedis_DbStillUpdated() throws Exception {
			// given
			// Redis에 추가 안 하고 DB만 있는 상황
			QueueEntry enteredEntry = queueEntryHelper.createQueueEntryWithRedis(testEvent, testUser, 10);
			enteredEntry.enterQueue();
			queueEntryRepository.save(enteredEntry);
			// Redis에는 추가 안 함

			// when & then
			mockMvc.perform(post("/api/v1/queues/{eventId}/move-to-back", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.newRank").value(11))
				.andDo(print());

			QueueEntry updatedEntry = queueEntryRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.getId())
				.orElseThrow();
			assertThat(updatedEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.WAITING);
			assertThat(updatedEntry.getQueueRank()).isEqualTo(11);

		}

	}
}
