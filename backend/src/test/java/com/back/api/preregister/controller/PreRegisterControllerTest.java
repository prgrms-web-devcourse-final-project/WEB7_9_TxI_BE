package com.back.api.preregister.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.domain.event.entity.Event;
import com.back.domain.event.repository.EventRepository;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;
import com.back.domain.preregister.repository.PreRegisterRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.EventErrorCode;
import com.back.global.error.code.PreRegisterErrorCode;
import com.back.support.data.TestUser;
import com.back.support.factory.EventFactory;
import com.back.support.factory.PreRegisterFactory;
import com.back.support.factory.PreRegisterRequestFactory;
import com.back.support.factory.UserFactory;
import com.back.support.helper.TestAuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PreRegisterController 통합 테스트")
class PreRegisterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PreRegisterRepository preRegisterRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private TestAuthHelper testAuthHelper;

	private static final String DEFAULT_PHONE_NUMBER = "01012345678";
	private static final String SMS_VERIFIED_KEY_PREFIX = "SMS_VERIFIED:";

	private TestUser testUser;
	private Event testEvent;
	private LocalDateTime now;

	@BeforeEach
	void setUp() {
		now = LocalDateTime.now();

		// Factory를 사용한 테스트 데이터 생성
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder);
		userRepository.save(testUser.user());

		testEvent = EventFactory.fakePreOpenEvent();
		eventRepository.save(testEvent);
	}

	private void setSmsVerified(String phoneNumber) {
		String verifiedKey = SMS_VERIFIED_KEY_PREFIX + phoneNumber;
		redisTemplate.opsForValue().set(verifiedKey, "true");
	}

	@Nested
	@DisplayName("사전등록 생성 API (POST /api/v1/events/{eventId}/pre-registers)")
	class RegisterPreRegister {

		@Test
		@DisplayName("유효한 본인 인증 정보로 사전등록 성공 후 DB에 저장된다")
		void register_Success() throws Exception {
			// given
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// when
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("사전등록이 완료되었습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.userId").value(testUser.user().getId()))
				.andExpect(jsonPath("$.data.status").value("REGISTERED"));

			// then: DB 검증
			PreRegister savedPreRegister = preRegisterRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.user().getId())
				.orElseThrow();

			assertThat(savedPreRegister.getEventId()).isEqualTo(testEvent.getId());
			assertThat(savedPreRegister.getUserId()).isEqualTo(testUser.user().getId());
			assertThat(savedPreRegister.getPreRegisterStatus()).isEqualTo(PreRegisterStatus.REGISTERED);
			assertThat(savedPreRegister.getPreRegisterAgreeTerms()).isTrue();
			assertThat(savedPreRegister.getPreRegisterAgreePrivacy()).isTrue();
		}

		@Test
		@DisplayName("생년월일이 일치하지 않으면 400 에러 (본인 인증 실패)")
		void register_Fail_InvalidBirthDate() throws Exception {
			// given: 잘못된 생년월일
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				LocalDate.of(2000, 1, 1)  // 실제 생년월일과 다름
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PreRegisterErrorCode.INVALID_USER_INFO.getMessage()));
		}

		@Test
		@DisplayName("이용약관 미동의 시 400 에러")
		void register_Fail_TermsNotAgreed() throws Exception {
			// given: 이용약관 미동의
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequestWithoutTerms(
				testUser.user().getBirthDate()
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PreRegisterErrorCode.TERMS_NOT_AGREED.getMessage()));
		}

		@Test
		@DisplayName("개인정보 수집 미동의 시 400 에러")
		void register_Fail_PrivacyNotAgreed() throws Exception {
			// given: 개인정보 수집 미동의
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequestWithoutPrivacy(
				testUser.user().getBirthDate()
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PreRegisterErrorCode.PRIVACY_NOT_AGREED.getMessage()));
		}

		@Test
		@DisplayName("중복 등록 시 400 에러")
		void register_Fail_AlreadyRegistered() throws Exception {
			// given: 이미 사전등록한 사용자
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			PreRegister existingPreRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			preRegisterRepository.save(existingPreRegister);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PreRegisterErrorCode.ALREADY_PRE_REGISTERED.getMessage()));
		}

		@Test
		@DisplayName("사전등록 기간이 아닐 때 400 에러")
		void register_Fail_InvalidPeriod() throws Exception {
			// given: 사전등록 기간이 지난 이벤트
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			Event closedEvent = EventFactory.fakePreClosedEvent();
			eventRepository.save(closedEvent);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", closedEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
					PreRegisterErrorCode.INVALID_PRE_REGISTRATION_PERIOD.getMessage()));
		}

	}

	@Nested
	@DisplayName("사전등록 취소 API (DELETE /api/v1/events/{eventId}/pre-registers)")
	class CancelPreRegister {

		@Test
		@DisplayName("사전등록 취소 성공 후 상태가 CANCELED로 변경된다")
		void cancel_Success() throws Exception {
			// given: 사전등록된 상태
			testAuthHelper.authenticate(testUser.user());

			PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			preRegisterRepository.save(preRegister);

			// when
			mockMvc.perform(delete("/api/v1/events/{eventId}/pre-registers", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.message").value("사전등록이 취소되었습니다."));

			// then: DB에서 상태 확인
			PreRegister canceledPreRegister = preRegisterRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.user().getId())
				.orElseThrow();

			assertThat(canceledPreRegister.getPreRegisterStatus()).isEqualTo(PreRegisterStatus.CANCELED);
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 404 에러")
		void cancel_Fail_NotFound() throws Exception {
			// when & then
			testAuthHelper.authenticate(testUser.user());

			mockMvc.perform(delete("/api/v1/events/{eventId}/pre-registers", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(
					PreRegisterErrorCode.NOT_FOUND_PRE_REGISTER.getMessage()));
		}

		@Test
		@DisplayName("이미 취소된 사전등록을 다시 취소하면 400 에러")
		void cancel_Fail_AlreadyCanceled() throws Exception {
			// given: 이미 취소된 사전등록
			testAuthHelper.authenticate(testUser.user());

			PreRegister preRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, testUser.user());
			preRegisterRepository.save(preRegister);

			// when & then
			mockMvc.perform(delete("/api/v1/events/{eventId}/pre-registers", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(PreRegisterErrorCode.ALREADY_CANCELED.getMessage()));
		}
	}

	@Nested
	@DisplayName("내 사전등록 조회 API (GET /api/v1/events/{eventId}/pre-registers/me)")
	class GetMyPreRegister {

		@Test
		@DisplayName("내 사전등록 조회 성공")
		void getMyPreRegister_Success() throws Exception {
			// given: 사전등록된 상태
			testAuthHelper.authenticate(testUser.user());

			PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			preRegisterRepository.save(preRegister);

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/me", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전등록 정보를 조회했습니다."))
				.andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
				.andExpect(jsonPath("$.data.userId").value(testUser.user().getId()))
				.andExpect(jsonPath("$.data.status").value("REGISTERED"));
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 404 에러")
		void getMyPreRegister_Fail_NotFound() throws Exception {
			// when & then
			testAuthHelper.authenticate(testUser.user());

			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/me", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(
					PreRegisterErrorCode.NOT_FOUND_PRE_REGISTER.getMessage()));
		}
	}

	@Nested
	@DisplayName("사전등록 여부 확인 API (GET /api/v1/events/{eventId}/pre-registers/status)")
	class IsRegistered {

		@Test
		@DisplayName("사전등록한 경우 true 반환")
		void isRegistered_True() throws Exception {
			// given: 사전등록된 상태
			testAuthHelper.authenticate(testUser.user());

			PreRegister preRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			preRegisterRepository.save(preRegister);

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/status", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(true));
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 false 반환")
		void isRegistered_False() throws Exception {
			// when & then
			testAuthHelper.authenticate(testUser.user());

			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/status", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(false));
		}

		@Test
		@DisplayName("취소된 사전등록은 false 반환")
		void isRegistered_False_WhenCanceled() throws Exception {
			// given: 취소된 사전등록
			testAuthHelper.authenticate(testUser.user());

			PreRegister preRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, testUser.user());
			preRegisterRepository.save(preRegister);

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/status", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(false));
		}
	}

	@Nested
	@DisplayName("사전등록 현황 조회 API (GET /api/v1/events/{eventId}/pre-registers/count)")
	class GetRegistrationCount {

		@Test
		@DisplayName("사전등록 수 조회 성공")
		void getRegistrationCount_Success() throws Exception {
			// given: 여러 사용자가 사전등록
			TestUser user1 = createUser();
			TestUser user2 = createUser();

			preRegisterRepository.save(createPreRegister(testEvent, user1.user()));
			preRegisterRepository.save(createPreRegister(testEvent, user2.user()));

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/count", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("사전등록 현황을 조회했습니다."))
				.andExpect(jsonPath("$.data").value(2));
		}

		@Test
		@DisplayName("취소된 사전등록은 카운트에서 제외된다")
		void getRegistrationCount_ExcludeCanceled() throws Exception {
			// given
			TestUser user1 = createUser();
			TestUser user2 = createUser();

			preRegisterRepository.save(createPreRegister(testEvent, user1.user()));

			PreRegister canceledPreRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, user2.user());
			preRegisterRepository.save(canceledPreRegister);

			// when & then: REGISTERED 상태만 카운트
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/count", testEvent.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(1));
		}

		@Test
		@DisplayName("존재하지 않는 이벤트는 404 에러")
		void getRegistrationCount_Fail_EventNotFound() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/count", 99999L))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("시나리오 및 엣지 케이스 테스트")
	class ScenarioAndEdgeCaseTests {

		@Test
		@DisplayName("취소 후 재등록 시나리오 - 성공")
		void scenario_CancelAndReRegister() throws Exception {
			// given: 먼저 사전등록
			testAuthHelper.authenticate(testUser.user());

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// 1. 등록
			setSmsVerified(DEFAULT_PHONE_NUMBER);
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("REGISTERED"));

			// 2. 취소
			mockMvc.perform(delete("/api/v1/events/{eventId}/pre-registers", testEvent.getId()))
				.andExpect(status().isNoContent());

			// 3. 취소 확인
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(false));

			// 4. 재등록
			setSmsVerified(DEFAULT_PHONE_NUMBER);
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", testEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("REGISTERED"));

			// 5. 재등록 확인
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/status", testEvent.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(true));
		}

		@Test
		@DisplayName("삭제된 이벤트에 사전등록 시도 시 404 에러")
		void register_Fail_DeletedEvent() throws Exception {
			// given: 삭제된 이벤트
			testAuthHelper.authenticate(testUser.user());
			setSmsVerified(DEFAULT_PHONE_NUMBER);

			Event deletedEvent = EventFactory.fakePreOpenEvent();
			eventRepository.save(deletedEvent);
			deletedEvent.delete();
			eventRepository.flush();

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// when & then
			mockMvc.perform(post("/api/v1/events/{eventId}/pre-registers", deletedEvent.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(EventErrorCode.NOT_FOUND_EVENT.getMessage()));
		}

		@Test
		@DisplayName("삭제된 이벤트에 대한 사전등록 현황 조회 시 404 에러")
		void getRegistrationCount_Fail_DeletedEvent() throws Exception {
			// given: 삭제된 이벤트
			Event deletedEvent = EventFactory.fakePreOpenEvent();
			eventRepository.save(deletedEvent);
			deletedEvent.delete();
			eventRepository.flush();

			// when & then
			mockMvc.perform(get("/api/v1/events/{eventId}/pre-registers/count", deletedEvent.getId()))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(EventErrorCode.NOT_FOUND_EVENT.getMessage()));
		}
	}

	// Helper methods
	private TestUser createUser() {
		TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder);
		userRepository.save(testUser.user());
		return testUser;
	}

	private PreRegister createPreRegister(Event event, User user) {
		return PreRegisterFactory.fakePreRegister(event, user);
	}
}
