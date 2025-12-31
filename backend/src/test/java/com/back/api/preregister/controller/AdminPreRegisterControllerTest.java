package com.back.api.preregister.controller;

import static org.hamcrest.Matchers.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.data.TestUser;
import com.back.support.factory.EventFactory;
import com.back.support.factory.PreRegisterFactory;
import com.back.support.factory.UserFactory;
import com.back.support.helper.StoreHelper;
import com.back.support.helper.TestAuthHelper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminPreRegisterController 통합 테스트")
class AdminPreRegisterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private PreRegisterRepository preRegisterRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestAuthHelper authHelper;

	@Autowired
	private StoreHelper storeHelper;

	private Event testEvent;
	private User adminUser;
	private String accessToken;
	private Store store;

	@BeforeEach
	void setUp() {
		store = storeHelper.createStore();
		testEvent = EventFactory.fakePreOpenEvent(store);
		eventRepository.save(testEvent);

		TestUser adminTestUser = UserFactory.fakeUser(UserRole.ADMIN, passwordEncoder, store);
		adminUser = adminTestUser.user();
		userRepository.save(adminUser);

		accessToken = authHelper.issueAccessToken(adminUser);
	}

	@Nested
	@DisplayName("이벤트별 사전 등록 목록 조회 API (GET /api/v1/admin/pre-registers/{eventId})")
	class GetPreRegistersByEventIdTests {

		@Test
		@DisplayName("사전 등록 목록 조회 성공 (페이징)")
		void getPreRegistersByEventId_Success() throws Exception {
			// given - 5명의 사용자가 사전 등록
			for (int i = 0; i < 5; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				User user = testUser.user();
				userRepository.save(user);

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, user);
				preRegisterRepository.save(preRegister);
			}

			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "0")
					.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전 등록 목록이 조회되었습니다."))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content", hasSize(5)))
				.andExpect(jsonPath("$.data.totalElements").value(5))
				.andExpect(jsonPath("$.data.number").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.pageable").exists())
				.andDo(print());
		}

		@Test
		@DisplayName("빈 사전 등록 목록 조회")
		void getPreRegistersByEventId_Empty() throws Exception {
			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "0")
					.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전 등록 목록이 조회되었습니다."))
				.andExpect(jsonPath("$.data.content", hasSize(0)))
				.andExpect(jsonPath("$.data.totalElements").value(0))
				.andDo(print());
		}

		@Test
		@DisplayName("페이지 크기 변경하여 조회")
		void getPreRegistersByEventId_CustomPageSize() throws Exception {
			// given - 25개 사전 등록 생성
			for (int i = 0; i < 25; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				User user = testUser.user();
				userRepository.save(user);

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, user);
				preRegisterRepository.save(preRegister);
			}

			// when then - 첫 페이지 (10개)
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "0")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(10)))
				.andExpect(jsonPath("$.data.totalElements").value(25))
				.andExpect(jsonPath("$.data.totalPages").value(3))
				.andExpect(jsonPath("$.data.number").value(0))
				.andDo(print());

			// when then - 두 번째 페이지
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(10)))
				.andExpect(jsonPath("$.data.number").value(1))
				.andDo(print());

			// when then - 마지막 페이지 (5개)
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "2")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(5)))
				.andExpect(jsonPath("$.data.number").value(2))
				.andDo(print());
		}

		@Test
		@DisplayName("기본 페이지 설정으로 조회 (page=0, size=20)")
		void getPreRegistersByEventId_DefaultPaging() throws Exception {
			// given
			for (int i = 0; i < 3; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				User user = testUser.user();
				userRepository.save(user);

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, user);
				preRegisterRepository.save(preRegister);
			}

			// when then - 파라미터 없이 요청
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(3)))
				.andExpect(jsonPath("$.data.number").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andDo(print());
		}

		@Test
		@DisplayName("취소된 사전 등록도 포함하여 조회")
		void getPreRegistersByEventId_IncludeCanceled() throws Exception {
			// given
			TestUser user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			TestUser user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			userRepository.save(user1.user());
			userRepository.save(user2.user());

			PreRegister activePreRegister = PreRegisterFactory.fakePreRegister(testEvent, user1.user());
			PreRegister canceledPreRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, user2.user());
			preRegisterRepository.save(activePreRegister);
			preRegisterRepository.save(canceledPreRegister);

			// when then - 모든 상태 포함
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken)
					.param("page", "0")
					.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", hasSize(1)))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("이벤트별 사전 등록 수 조회 API (GET /api/v1/admin/pre-registers/{eventId}/count)")
	class GetPreRegisterCountTests {

		@Test
		@DisplayName("사전 등록 수 조회 성공")
		void getPreRegisterCountByEventId_Success() throws Exception {
			for (int i = 0; i < 10; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				User user = testUser.user();
				userRepository.save(user);

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, user);
				preRegisterRepository.save(preRegister);
			}

			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}/count", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전 등록 수가 조회되었습니다."))
				.andExpect(jsonPath("$.data").value(10))
				.andDo(print());
		}

		@Test
		@DisplayName("사전 등록이 없는 경우 0 반환")
		void getPreRegisterCountByEventId_ZeroCount() throws Exception {
			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}/count", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전 등록 수가 조회되었습니다."))
				.andExpect(jsonPath("$.data").value(0))
				.andDo(print());
		}

		@Test
		@DisplayName("취소된 사전 등록은 카운트에서 제외")
		void getPreRegisterCountByEventId_ExcludeCanceled() throws Exception {
			// given
			TestUser user1 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			TestUser user2 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			TestUser user3 = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			userRepository.save(user1.user());
			userRepository.save(user2.user());
			userRepository.save(user3.user());

			// 2명은 REGISTERED, 1명은 CANCELED
			PreRegister activePreRegister1 = PreRegisterFactory.fakePreRegister(testEvent, user1.user());
			PreRegister activePreRegister2 = PreRegisterFactory.fakePreRegister(testEvent, user2.user());
			PreRegister canceledPreRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, user3.user());

			preRegisterRepository.save(activePreRegister1);
			preRegisterRepository.save(activePreRegister2);
			preRegisterRepository.save(canceledPreRegister);

			// when then - REGISTERED 상태만 카운트
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}/count", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(2))
				.andDo(print());
		}

		@Test
		@DisplayName("대량 사전 등록 카운트")
		void getPreRegisterCountByEventId_LargeCount() throws Exception {

			for (int i = 0; i < 100; i++) {
				TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
				User user = testUser.user();
				userRepository.save(user);

				PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, user);
				preRegisterRepository.save(preRegister);
			}

			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}/count", testEvent.getId())
					.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(100))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("권한 검증 테스트")
	class AuthorizationTests {

		@Test
		@DisplayName("일반 사용자는 접근 불가 (403)")
		void getPreRegisters_Forbidden_NormalUser() throws Exception {
			TestUser normalUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			userRepository.save(normalUser.user());
			String normalUserToken = authHelper.issueAccessToken(normalUser.user());

			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.header("Authorization", "Bearer " + normalUserToken)
					.param("page", "0")
					.param("size", "20"))
				.andExpect(status().isForbidden())
				.andDo(print());
		}

		@Test
		@DisplayName("인증 없이 접근 시 401")
		void getPreRegisters_Unauthorized_NoToken() throws Exception {
			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}", testEvent.getId())
					.param("page", "0")
					.param("size", "20"))
				.andExpect(status().isUnauthorized())
				.andDo(print());
		}

		@Test
		@DisplayName("일반 사용자는 카운트 조회도 불가 (403)")
		void getPreRegisterCount_Forbidden_NormalUser() throws Exception {
			TestUser normalUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
			userRepository.save(normalUser.user());
			String normalUserToken = authHelper.issueAccessToken(normalUser.user());

			// when then
			mockMvc.perform(get("/api/v1/admin/pre-registers/{eventId}/count", testEvent.getId())
					.header("Authorization", "Bearer " + normalUserToken))
				.andExpect(status().isForbidden())
				.andDo(print());
		}
	}
}