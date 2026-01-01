package com.back.api.user.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.event.entity.Event;
import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;
import com.back.domain.queue.repository.QueueEntryRedisRepository;
import com.back.domain.queue.repository.QueueEntryRepository;
import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.UserErrorCode;
import com.back.global.http.HttpRequestContext;
import com.back.support.data.TestUser;
import com.back.support.helper.EventHelper;
import com.back.support.helper.QueueEntryHelper;
import com.back.support.helper.SeatHelper;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TestAuthHelper;
import com.back.support.helper.TicketHelper;
import com.back.support.helper.UserHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private ActiveSessionRepository activeSessionRepository;

	@Autowired
	private QueueEntryRepository queueEntryRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private TestAuthHelper testAuthHelper;

	@Autowired
	private TicketHelper ticketHelper;

	@Autowired
	private SeatHelper seatHelper;

	@Autowired
	private QueueEntryHelper queueEntryHelper;

	@Autowired
	private EventHelper eventHelper;

	@Autowired
	private StoreHelper storeHelper;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private QueueEntryRedisRepository queueEntryRedisRepository;

	@Autowired
	private com.back.api.auth.service.ActiveSessionCache activeSessionCache;

	@MockitoSpyBean
	private RefreshTokenRepository spyRefreshTokenRepository;

	@MockitoSpyBean
	private HttpRequestContext requestContext;

	private User user;

	private final ObjectMapper mapper = new ObjectMapper();

	TestUser testUser;

	String token;

	Event testEvent;

	Seat seat;

	Store store;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();
		testUser = userHelper.createUser(UserRole.NORMAL, null);
		user = userRepository.findById(testUser.user().getId()).orElseThrow();

		token = testAuthHelper.issueAccessToken(user);

		entityManager.flush();
		entityManager.clear();
		user = userRepository.findById(user.getId()).orElseThrow();

		testAuthHelper.clearAuthentication();

		testEvent = eventHelper.createEvent(store, "TestEvent");
		queueEntryRedisRepository.clearAll(testEvent.getId());
		seat = seatHelper.createSeat(testEvent, "A1", SeatGrade.VIP);
	}

	@Nested
	@DisplayName("GET `/api/v1/users/profile`")
	class GetUserTest {

		private final String getUserApi = "/api/v1/users/profile";

		@Test
		@DisplayName("Success get user profile info")
		void get_user_profile_success() throws Exception {
			long userId = user.getId();

			ResultActions actions = mvc
				.perform(
					get(getUserApi)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 조회 성공", userId)));
		}

		@Test
		@DisplayName("Failed by not allowed user")
		void failed_by_not_allowed_user() throws Exception {
			ResultActions actions = mvc.perform(get(getUserApi))
				.andDo(print());

			AuthErrorCode error = AuthErrorCode.UNAUTHORIZED;

			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}

	@Nested
	@DisplayName("사용자 프로필 수정 - `PUT /api/v1/users/profile`")
	class UpdateProfileTest {
		private final String updateProfileApi = "/api/v1/users/profile";

		@Test
		@DisplayName("사용자 정보 변경 성공")
		void success_update_profile() throws Exception {
			long userId = user.getId();

			String newNickname = "New" + user.getNickname();
			String newFullName = "New" + user.getFullName();

			String requestJson = mapper.writeValueAsString(Map.of(
				"fullName", newFullName,
				"nickname", newNickname,
				"year", "2002",
				"month", "2",
				"day", "11"
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(newNickname).isEqualTo(updatedUser.getNickname());
			assertThat(newFullName).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 성공 - 파라미터 일부만 전달")
		void success_with_missing_parameters() throws Exception {
			long userId = user.getId();

			String newNickname = "New" + user.getNickname();

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", newNickname
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(newNickname).isEqualTo(updatedUser.getNickname());
			assertThat(user.getFullName()).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 성공 - 파라미터 없음")
		void success_with_empty_request() throws Exception {
			long userId = user.getId();

			String requestJson = mapper.writeValueAsString(Map.of(
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(user.getNickname()).isEqualTo(updatedUser.getNickname());
			assertThat(user.getFullName()).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 실패 - 인증 실패")
		void failed_update_profile_by_user_not_found() throws Exception {
			// given
			long userId = user.getId();

			deleteUserById(userId);

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", "newName"
			));

			// when
			ResultActions actions = mvc.perform(
				put(updateProfileApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// then
			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(AuthErrorCode.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message")
					.value(AuthErrorCode.UNAUTHORIZED.getMessage()));
		}

		@Test
		@DisplayName("사용자 정보 변경 실패 - 닉네임 중복 (ALREADY_EXIST_NICKNAME)")
		void failed_update_profile_by_duplicate_nickname() throws Exception {
			// 이미 존재하는 다른 유저 생성
			TestUser anotherUser = userHelper.createUser(UserRole.NORMAL, null);
			String duplicatedNickname = anotherUser.user().getNickname();

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", duplicatedNickname
			));

			// when
			ResultActions actions = mvc.perform(
				put(updateProfileApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// then
			actions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
				.andExpect(jsonPath("$.message")
					.value(AuthErrorCode.ALREADY_EXIST_NICKNAME.getMessage()));
		}
	}

	@Nested
	@DisplayName("회원탈퇴 - `DELETE /api/v1/users/me`")
	class DeleteUserTest {

		private final String deleteUserApi = "/api/v1/users/me";

		@Test
		@DisplayName("회원탈퇴 성공 - 쿠키 삭제 + soft delete + (있다면) refreshToken 전체 revoke")
		void delete_user_success_with_revoke_all() throws Exception {
			// given
			long userId = user.getId();

			// (선택) 현재 유저의 refresh token이 존재하는지 확인만 해두기 (없어도 테스트 진행)
			boolean hasActiveTokenBefore =
				refreshTokenRepository.findByUserIdAndRevokedFalse(userId).isPresent();

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.name()))
				.andExpect(jsonPath("$.message").value("회원탈퇴 완료"))
				.andExpect(cookie().maxAge("accessToken", 0))
				.andExpect(cookie().maxAge("refreshToken", 0));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNotNull();

			// then (RefreshToken revoke 검증: 토큰이 있던 경우에만 의미 있게 검증)
			if (hasActiveTokenBefore) {
				assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).isEmpty();
			}
		}

		@Test
		@DisplayName("회원탈퇴 실패 - 사용자 없음 (NOT_FOUND)")
		void delete_user_failed_by_not_found() throws Exception {
			// given
			long userId = user.getId();

			deleteUserById(userId);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(AuthErrorCode.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(AuthErrorCode.UNAUTHORIZED.getMessage()));
		}

		@Test
		@DisplayName("회원탈퇴 실패 - revokeAll/쿠키삭제 등 부작용이 없어야 한다")
		void delete_user_failed_by_not_found_has_no_side_effects() throws Exception {
			// given
			long userId = user.getId();

			// authenticate 과정에서 spy 호출이 발생할 수 있으니, 검증 전에 기록 제거
			clearInvocations(spyRefreshTokenRepository, requestContext);

			// NOT_FOUND 유도: 인증은 유지하되, DB에서 유저만 삭제
			activeSessionRepository.deleteByUserId(userId);
			userRepository.deleteById(userId);
			userRepository.flush();
			// NOT_FOUND 유도: Redis 캐시 + DB에서 ActiveSession + User 삭제
			activeSessionCache.evict(userId);
			activeSessionRepository.deleteByUserId(userId);
			userRepository.deleteById(userId);
			userRepository.flush();

			// when
			ResultActions actions = mvc.perform(
				delete("/api/v1/users/me")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then: 응답 검증
			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(AuthErrorCode.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(AuthErrorCode.UNAUTHORIZED.getMessage()));

			// then: 부작용(토큰 revoke / 쿠키 삭제) 없어야 함
			verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
			verify(requestContext, never()).deleteCookie("accessToken");
			verify(requestContext, never()).deleteCookie("refreshToken");
		}

		@Test
		@DisplayName("회원탈퇴 성공 - 대기열 WAITING 상태, 탈퇴 후 QueueEntry Expired 처리")
		void success_delete_user_in_waiting_queue_and_update_to_expired() throws Exception {
			// given
			long userId = user.getId();
			queueEntryHelper.createQueueEntryWithRedis(testEvent, user, 1);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.name()))
				.andExpect(jsonPath("$.message").value("회원탈퇴 완료"))
				.andExpect(cookie().maxAge("accessToken", 0))
				.andExpect(cookie().maxAge("refreshToken", 0));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNotNull();

			QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(testEvent.getId(), userId)
				.orElseThrow(() -> new RuntimeException("Not found queue entry"));
			assertThat(queueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.EXPIRED);
		}

		@Test
		@DisplayName("회원탈퇴 성공 - 대기열 ENTERED 상태, 탈퇴 후 QueueEntry Expired 처리")
		void success_delete_user_in_entered_queue_and_update_to_expired() throws Exception {
			// given
			long userId = user.getId();
			queueEntryHelper.createEnteredQueueEntryWithRedis(testEvent, user);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.name()))
				.andExpect(jsonPath("$.message").value("회원탈퇴 완료"))
				.andExpect(cookie().maxAge("accessToken", 0))
				.andExpect(cookie().maxAge("refreshToken", 0));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNotNull();

			QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(testEvent.getId(), userId)
				.orElseThrow(() -> new RuntimeException("Not found queue entry"));
			assertThat(queueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.EXPIRED);
		}

		@Test
		@DisplayName("회원탈퇴 실패 - 대기열 ENTERED 상태, 티켓 구매 완료, 이벤트 행사 전 탈퇴 요청")
		void failed_delete_user_in_completed_queue_with_paid_ticket() throws Exception {
			// given
			long userId = user.getId();
			queueEntryHelper.createCompletedQueueEntry(testEvent, user);
			ticketHelper.createPaidTicket(user, seat, testEvent);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
				.andExpect(jsonPath("$.message").value(UserErrorCode.CAN_NOT_DELETE_USER.getMessage()));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNull();

			QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(testEvent.getId(), userId)
				.orElseThrow(() -> new RuntimeException("Not found queue entry"));
			assertThat(queueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.COMPLETED);
		}

		@Test
		@DisplayName("회원탈퇴 실패 - 대기열 ENTERED 상태, 티켓 상태 ISSUED, 이벤트 행사 전 탈퇴 요청")
		void failed_delete_user_in_completed_queue_with_issued_ticket() throws Exception {
			// given
			long userId = user.getId();
			queueEntryHelper.createCompletedQueueEntry(testEvent, user);
			ticketHelper.createIssuedTicket(user, seat, testEvent);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
				.andExpect(jsonPath("$.message").value(UserErrorCode.CAN_NOT_DELETE_USER.getMessage()));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNull();

			QueueEntry queueEntry = queueEntryRepository.findByEvent_IdAndUser_Id(testEvent.getId(), userId)
				.orElseThrow(() -> new RuntimeException("Not found queue entry"));
			assertThat(queueEntry.getQueueEntryStatus()).isEqualTo(QueueEntryStatus.COMPLETED);
		}

		@Test
		@DisplayName("회원탈퇴 성공 - 티켓을 구매완료, 행사가 완료됨")
		void success_delete_user_with_past_event() throws Exception {
			// given
			long userId = user.getId();
			testEvent = eventHelper.createPastEvent(store, "PastEvent");
			queueEntryHelper.createCompletedQueueEntry(testEvent, user);
			ticketHelper.createPaidTicket(user, seat, testEvent);

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.name()))
				.andExpect(jsonPath("$.message").value("회원탈퇴 완료"))
				.andExpect(cookie().maxAge("accessToken", 0))
				.andExpect(cookie().maxAge("refreshToken", 0));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNotNull();
		}
	}

	private void deleteUserById(long userId) {
		// Redis 캐시 삭제 (DB와 동기화)
		activeSessionCache.evict(userId);

		activeSessionRepository.deleteByUserId(userId);
		activeSessionRepository.flush();

		userRepository.deleteById(userId);
		userRepository.flush();
	}
}
