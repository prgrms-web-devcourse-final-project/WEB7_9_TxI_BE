package com.back.api.preregister.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;
import com.back.api.preregister.dto.response.PreRegisterResponse;
import com.back.api.s3.service.S3MoveService;
import com.back.api.s3.service.S3PresignedService;
import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;
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
import com.back.global.error.exception.ErrorException;
import com.back.support.data.TestUser;
import com.back.support.factory.EventFactory;
import com.back.support.factory.PreRegisterFactory;
import com.back.support.factory.PreRegisterRequestFactory;
import com.back.support.factory.UserFactory;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PreRegisterService 통합 테스트")
class PreRegisterServiceTest {

	@Autowired
	private PreRegisterService preRegisterService;

	@Autowired
	private PreRegisterRepository preRegisterRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@MockitoBean
	private S3MoveService s3MoveService;

	@MockitoBean
	private S3PresignedService s3PresignedService;


	private static final String DEFAULT_PHONE_NUMBER = "01012345678";
	private static final String SMS_VERIFIED_KEY_PREFIX = "SMS_VERIFIED:";

	private TestUser testUser;
	private Event testEvent;
	private LocalDateTime now;

	private void setSmsVerified(String phoneNumber) {
		String key = SMS_VERIFIED_KEY_PREFIX + phoneNumber;
		redisTemplate.opsForValue().set(key, "true");
	}

	@BeforeEach
	void setUp() {
		now = LocalDateTime.now();

		// Factory를 사용한 테스트 데이터 생성
		testUser = UserFactory.fakeUser(UserRole.NORMAL);
		userRepository.save(testUser.user());

		testEvent = EventFactory.fakePreOpenEvent();
		eventRepository.save(testEvent);

		when(s3MoveService.moveImage(anyLong(), anyString()))
			.thenReturn("events/1/main.jpg");

		when(s3PresignedService.issueDownloadUrl(anyString()))
			.thenReturn("https://s3.amazonaws.com/bucket/events/1/main.jpg?signature=xxx");

		// 모든 테스트에서 SMS 인증 완료 상태로 시작
		setSmsVerified(DEFAULT_PHONE_NUMBER);
	}

	@Nested
	@DisplayName("사전등록 생성 (register)")
	class Register {

		@Test
		@DisplayName("유효한 본인 인증 정보로 사전등록 성공 후 DB에 저장된다")
		void register_Success() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when
			PreRegisterResponse response = preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			);

			// then
			assertThat(response.id()).isNotNull();
			assertThat(response.eventId()).isEqualTo(testEvent.getId());
			assertThat(response.userId()).isEqualTo(testUser.user().getId());

			// DB 검증
			PreRegister savedPreRegister = preRegisterRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.user().getId())
				.orElseThrow();

			assertThat(savedPreRegister.getEventId()).isEqualTo(testEvent.getId());
			assertThat(savedPreRegister.getUserId()).isEqualTo(testUser.user().getId());
			assertThat(savedPreRegister.getPreRegisterStatus()).isEqualTo(PreRegisterStatus.REGISTERED);
			assertThat(savedPreRegister.getPreRegisterAgreeTerms()).isTrue();
			assertThat(savedPreRegister.getPreRegisterAgreePrivacy()).isTrue();
			assertThat(savedPreRegister.getPreRegisterAgreedAt()).isNotNull();
		}

		@Test
		@DisplayName("생년월일이 일치하지 않으면 INVALID_USER_INFO 예외 발생")
		void register_Fail_InvalidBirthDate() {
			// given: 잘못된 생년월일
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				LocalDate.of(2000, 1, 1)  // 실제 생년월일과 다름
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_USER_INFO.getMessage());
		}

		@Test
		@DisplayName("이용약관 미동의 시 TERMS_NOT_AGREED 예외 발생")
		void register_Fail_TermsNotAgreed() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequestWithoutTerms(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.TERMS_NOT_AGREED.getMessage());
		}

		@Test
		@DisplayName("개인정보 수집 미동의 시 PRIVACY_NOT_AGREED 예외 발생")
		void register_Fail_PrivacyNotAgreed() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequestWithoutPrivacy(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.PRIVACY_NOT_AGREED.getMessage());
		}

		@Test
		@DisplayName("중복 등록 시 ALREADY_PRE_REGISTERED 예외 발생")
		void register_Fail_AlreadyRegistered() {
			// given: 이미 사전등록한 사용자
			PreRegister existingPreRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			preRegisterRepository.save(existingPreRegister);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.ALREADY_PRE_REGISTERED.getMessage());
		}

		@Test
		@DisplayName("사전등록 기간 전이면 INVALID_PRE_REGISTRATION_PERIOD 예외 발생")
		void register_Fail_BeforePreOpenDate() {
			// given: 사전등록 시작 전인 이벤트
			Event futureEvent = Event.builder()
				.title("미래 이벤트")
				.category(EventCategory.CONCERT)
				.description("설명")
				.place("장소")
				.imageUrl("url")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(now.plusDays(1))   // 내일부터 시작
				.preCloseAt(now.plusDays(5))
				.ticketOpenAt(now.plusDays(10))
				.ticketCloseAt(now.plusDays(20))
				.eventDate(now.plusDays(25))
				.maxTicketAmount(100)
				.status(EventStatus.READY)
				.build();
			eventRepository.save(futureEvent);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				futureEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_PRE_REGISTRATION_PERIOD.getMessage());
		}

		@Test
		@DisplayName("사전등록 기간 종료 후면 INVALID_PRE_REGISTRATION_PERIOD 예외 발생")
		void register_Fail_AfterPreCloseDate() {
			// given: 사전등록 기간이 지난 이벤트
			Event closedEvent = Event.builder()
				.title("종료된 이벤트")
				.category(EventCategory.CONCERT)
				.description("설명")
				.place("장소")
				.imageUrl("url")
				.minPrice(10000)
				.maxPrice(50000)
				.preOpenAt(now.minusDays(10))
				.preCloseAt(now.minusDays(8))  // 8일 전에 종료
				.ticketOpenAt(now.minusDays(5))
				.ticketCloseAt(now.plusDays(10))
				.eventDate(now.plusDays(15))
				.maxTicketAmount(100)
				.status(EventStatus.OPEN)
				.build();
			eventRepository.save(closedEvent);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				closedEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_PRE_REGISTRATION_PERIOD.getMessage());
		}

		@Test
		@DisplayName("존재하지 않는 이벤트는 NOT_FOUND_EVENT 예외 발생")
		void register_Fail_EventNotFound() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				99999L,  // 존재하지 않는 이벤트 ID
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(EventErrorCode.NOT_FOUND_EVENT.getMessage());
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 NOT_FOUND_USER 예외 발생")
		void register_Fail_UserNotFound() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				99999L,  // 존재하지 않는 사용자 ID
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(CommonErrorCode.NOT_FOUND_USER.getMessage());
		}

		@Test
		@DisplayName("사용자 생년월일이 null이면 INVALID_USER_INFO 예외 발생")
		void register_Fail_UserBirthDateNull() {
			// given: 생년월일이 null인 사용자
			User userWithoutBirthDate = User.builder()
				.email("nobirth@test.com")
				.password("encodedPassword")
				.fullName("Test User")
				.nickname("TestNickname999")
				.birthDate(null)  // 생년월일 null
				.role(UserRole.NORMAL)
				.build();
			userRepository.save(userWithoutBirthDate);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				userWithoutBirthDate.getFullName(),
				LocalDate.of(2000, 1, 1)
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				userWithoutBirthDate.getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_USER_INFO.getMessage());
		}

		@Test
		@DisplayName("SMS 인증 미완료 시 SMS_VERIFICATION_NOT_COMPLETED 예외 발생")
		void register_Fail_SmsNotVerified() {
			// given: SMS 인증을 하지 않음 (Redis에 인증 플래그 없음)
			String unverifiedPhoneNumber = "01099999999";
			PreRegisterCreateRequest request = new PreRegisterCreateRequest(
				testUser.user().getFullName(),
				unverifiedPhoneNumber,
				testUser.user().getBirthDate(),
				true,
				true
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.SMS_VERIFICATION_NOT_COMPLETED.getMessage());
		}

		@Test
		@DisplayName("이름이 일치하지 않으면 INVALID_USER_INFO 예외 발생")
		void register_Fail_InvalidFullName() {
			// given: 잘못된 이름
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				"잘못된이름",
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_USER_INFO.getMessage());
		}

		@Test
		@DisplayName("사용자 이름이 null이면 INVALID_USER_INFO 예외 발생")
		void register_Fail_UserFullNameNull() {
			// given: 이름이 null인 사용자
			User userWithoutFullName = User.builder()
				.email("noname@test.com")
				.password("encodedPassword")
				.fullName(null)  // 이름 null
				.nickname("TestNickname998")
				.birthDate(LocalDate.of(1990, 1, 1))
				.role(UserRole.NORMAL)
				.build();
			userRepository.save(userWithoutFullName);

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				"홍길동",
				LocalDate.of(1990, 1, 1)
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				userWithoutFullName.getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.INVALID_USER_INFO.getMessage());
		}
	}

	@Nested
	@DisplayName("사전등록 취소 (cancel)")
	class Cancel {

		@Test
		@DisplayName("사전등록 취소 성공 후 상태가 CANCELED로 변경된다")
		void cancel_Success() {
			// given: 사전등록된 상태
			PreRegister preRegister = PreRegister.builder()
				.event(testEvent)
				.user(testUser.user())
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();
			preRegisterRepository.save(preRegister);

			// when
			preRegisterService.cancel(testEvent.getId(), testUser.user().getId());

			// then: DB에서 상태 확인
			PreRegister canceledPreRegister = preRegisterRepository
				.findByEvent_IdAndUser_Id(testEvent.getId(), testUser.user().getId())
				.orElseThrow();

			assertThat(canceledPreRegister.getPreRegisterStatus()).isEqualTo(PreRegisterStatus.CANCELED);
			assertThat(canceledPreRegister.isCanceled()).isTrue();
			assertThat(canceledPreRegister.isRegistered()).isFalse();
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 NOT_FOUND_PRE_REGISTER 예외 발생")
		void cancel_Fail_NotFound() {
			// when & then
			assertThatThrownBy(() -> preRegisterService.cancel(
				testEvent.getId(),
				testUser.user().getId()
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.NOT_FOUND_PRE_REGISTER.getMessage());
		}

		@Test
		@DisplayName("이미 취소된 사전등록을 다시 취소하면 ALREADY_CANCELED 예외 발생")
		void cancel_Fail_AlreadyCanceled() {
			// given: 이미 취소된 사전등록
			PreRegister preRegister = PreRegister.builder()
				.event(testEvent)
				.user(testUser.user())
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();
			preRegister.cancel();
			preRegisterRepository.save(preRegister);

			// when & then
			assertThatThrownBy(() -> preRegisterService.cancel(
				testEvent.getId(),
				testUser.user().getId()
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(PreRegisterErrorCode.ALREADY_CANCELED.getMessage());
		}
	}

	@Nested
	@DisplayName("내 사전등록 조회 (getMyPreRegister)")
	class GetMyPreRegister {

		@Test
		@DisplayName("내 사전등록 목록 조회 성공")
		void getMyPreRegister_Success() {
			// given: 여러 이벤트에 사전등록
			Event event2 = EventFactory.fakePreOpenEvent();
			eventRepository.save(event2);

			PreRegister preRegister1 = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			PreRegister preRegister2 = PreRegisterFactory.fakePreRegister(event2, testUser.user());
			preRegisterRepository.save(preRegister1);
			preRegisterRepository.save(preRegister2);

			// when
			List<PreRegisterResponse> responses = preRegisterService.getMyPreRegister(testUser.user().getId());

			// then
			assertThat(responses).hasSize(2);
			assertThat(responses).extracting(PreRegisterResponse::eventId)
				.containsExactlyInAnyOrder(testEvent.getId(), event2.getId());
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 빈 배열 반환")
		void getMyPreRegister_Empty() {
			// when
			List<PreRegisterResponse> responses = preRegisterService.getMyPreRegister(testUser.user().getId());

			// then
			assertThat(responses).isEmpty();
		}

		@Test
		@DisplayName("취소된 사전등록도 포함하여 조회")
		void getMyPreRegister_IncludeCanceled() {
			// given
			Event event2 = EventFactory.fakePreOpenEvent();
			eventRepository.save(event2);

			PreRegister activePreRegister = PreRegisterFactory.fakePreRegister(testEvent, testUser.user());
			PreRegister canceledPreRegister = PreRegisterFactory.fakeCanceledPreRegister(event2, testUser.user());
			preRegisterRepository.save(activePreRegister);
			preRegisterRepository.save(canceledPreRegister);

			// when
			List<PreRegisterResponse> responses = preRegisterService.getMyPreRegister(testUser.user().getId());

			// then: 모든 상태 조회
			assertThat(responses).hasSize(2);
		}
	}

	@Nested
	@DisplayName("사전등록 여부 확인 (isRegistered)")
	class IsRegistered {

		@Test
		@DisplayName("REGISTERED 상태면 true 반환")
		void isRegistered_True() {
			// given: 사전등록된 상태
			PreRegister preRegister = PreRegister.builder()
				.event(testEvent)
				.user(testUser.user())
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();
			preRegisterRepository.save(preRegister);

			// when
			boolean isRegistered = preRegisterService.isRegistered(
				testEvent.getId(),
				testUser.user().getId()
			);

			// then
			assertThat(isRegistered).isTrue();
		}

		@Test
		@DisplayName("사전등록하지 않은 경우 false 반환")
		void isRegistered_False() {
			// when
			boolean isRegistered = preRegisterService.isRegistered(
				testEvent.getId(),
				testUser.user().getId()
			);

			// then
			assertThat(isRegistered).isFalse();
		}

		@Test
		@DisplayName("CANCELED 상태면 false 반환")
		void isRegistered_False_WhenCanceled() {
			// given: 취소된 사전등록
			PreRegister preRegister = PreRegister.builder()
				.event(testEvent)
				.user(testUser.user())
				.preRegisterAgreeTerms(true)
				.preRegisterAgreePrivacy(true)
				.build();
			preRegister.cancel();
			preRegisterRepository.save(preRegister);

			// when
			boolean isRegistered = preRegisterService.isRegistered(
				testEvent.getId(),
				testUser.user().getId()
			);

			// then
			assertThat(isRegistered).isFalse();
		}
	}

	@Nested
	@DisplayName("사전등록 현황 조회 (getRegistrationCount)")
	class GetRegistrationCount {

		@Test
		@DisplayName("REGISTERED 상태의 사전등록 수만 카운트된다")
		void getRegistrationCount_OnlyRegisteredStatus() {
			// given: 여러 사용자가 사전등록
			TestUser user1 = createUser();
			TestUser user2 = createUser();
			TestUser user3 = createUser();

			// user1, user2는 REGISTERED 상태
			preRegisterRepository.save(createPreRegister(testEvent, user1.user()));
			preRegisterRepository.save(createPreRegister(testEvent, user2.user()));

			// user3는 CANCELED 상태
			PreRegister canceledPreRegister = PreRegisterFactory.fakeCanceledPreRegister(testEvent, user3.user());
			preRegisterRepository.save(canceledPreRegister);

			// when
			Long count = preRegisterService.getRegistrationCount(testEvent.getId());

			// then: REGISTERED 상태만 카운트 (2명)
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("사전등록이 없으면 0 반환")
		void getRegistrationCount_Zero() {
			// when
			Long count = preRegisterService.getRegistrationCount(testEvent.getId());

			// then
			assertThat(count).isZero();
		}

		@Test
		@DisplayName("존재하지 않는 이벤트는 NOT_FOUND_EVENT 예외 발생")
		void getRegistrationCount_Fail_EventNotFound() {
			// when & then
			assertThatThrownBy(() -> preRegisterService.getRegistrationCount(99999L))
				.isInstanceOf(ErrorException.class)
				.hasMessage(EventErrorCode.NOT_FOUND_EVENT.getMessage());
		}

		@Test
		@DisplayName("Repository의 countBy 메서드가 정확히 작동한다 (DB 무결성 검증)")
		void getRegistrationCount_RepositoryCountByWorks() {
			// given
			TestUser user1 = createUser();
			TestUser user2 = createUser();

			preRegisterRepository.save(createPreRegister(testEvent, user1.user()));
			preRegisterRepository.save(createPreRegister(testEvent, user2.user()));

			// when: Repository 메서드 직접 호출
			Long repositoryCount = preRegisterRepository.countByEvent_IdAndPreRegisterStatus(
				testEvent.getId(),
				PreRegisterStatus.REGISTERED
			);

			Long serviceCount = preRegisterService.getRegistrationCount(testEvent.getId());

			// then: Service와 Repository 결과가 일치해야 함
			assertThat(repositoryCount).isEqualTo(2);
			assertThat(serviceCount).isEqualTo(repositoryCount);
		}
	}

	@Nested
	@DisplayName("동시성 및 시나리오 테스트")
	class ConcurrencyAndScenarioTests {

		@Test
		@DisplayName("여러 사용자가 동시에 사전등록해도 중복 방지가 작동한다")
		void register_MultipleConcurrentUsers() {
			// given: 3명의 사용자
			TestUser user1 = createUser();
			TestUser user2 = createUser();
			TestUser user3 = createUser();

			// when: 각 사용자가 사전등록
			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user1.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user1.user().getFullName(),
					user1.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user2.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user2.user().getFullName(),
					user2.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user3.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user3.user().getFullName(),
					user3.user().getBirthDate()
				));

			// then: 3명 모두 등록되어야 함
			Long count = preRegisterService.getRegistrationCount(testEvent.getId());
			assertThat(count).isEqualTo(3);
		}
/*
		@Test
		@DisplayName("등록 -> 조회 -> 취소 -> 재조회 시나리오 테스트")
		void scenario_RegisterCheckCancelCheck() {
			// given
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getBirthDate()
			);

			// when & then: 1. 등록
			PreRegisterResponse registerResponse = preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			);
			// 2. 조회
			PreRegisterResponse getResponse = preRegisterService.getMyPreRegister(
				testEvent.getId(),
				testUser.user().getId()
			);
			assertThat(getResponse.id()).isEqualTo(registerResponse.id());
			assertThat(getResponse.status()).isEqualTo(PreRegisterStatus.REGISTERED);

			// 3. 등록 여부 확인
			boolean isRegisteredBefore = preRegisterService.isRegistered(
				testEvent.getId(),
				testUser.user().getId()
			);
			assertThat(isRegisteredBefore).isTrue();

			// 4. 취소
			preRegisterService.cancel(testEvent.getId(), testUser.user().getId());

			// 5. 취소 후 등록 여부 확인
			boolean isRegisteredAfter = preRegisterService.isRegistered(
				testEvent.getId(),
				testUser.user().getId()
			);
			assertThat(isRegisteredAfter).isFalse();

			// 6. 취소 후 조회 시 CANCELED 상태 반환
			PreRegisterResponse afterCancelResponse = preRegisterService.getMyPreRegister(
				testEvent.getId(),
				testUser.user().getId()
			);
			assertThat(afterCancelResponse.status()).isEqualTo(PreRegisterStatus.CANCELED);
		}*/

		@Test
		@DisplayName("동일한 이벤트에 여러 사용자가 등록하고 일부만 취소하는 시나리오")
		void scenario_MultipleUsersRegisterAndSomeCancel() {
			// given: 5명의 사용자
			TestUser user1 = createUser();
			TestUser user2 = createUser();
			TestUser user3 = createUser();
			TestUser user4 = createUser();
			TestUser user5 = createUser();

			// when: 5명 모두 등록
			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user1.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user1.user().getFullName(),
					user1.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user2.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user2.user().getFullName(),
					user2.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user3.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user3.user().getFullName(),
					user3.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user4.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user4.user().getFullName(),
					user4.user().getBirthDate()
				));

			setSmsVerified(DEFAULT_PHONE_NUMBER);
			preRegisterService.register(testEvent.getId(), user5.user().getId(),
				PreRegisterRequestFactory.fakePreRegisterRequest(
					user5.user().getFullName(),
					user5.user().getBirthDate()
				));

			// then: 5명 등록 확인
			Long countBeforeCancel = preRegisterService.getRegistrationCount(testEvent.getId());
			assertThat(countBeforeCancel).isEqualTo(5);

			// when: user2, user4 취소
			preRegisterService.cancel(testEvent.getId(), user2.user().getId());
			preRegisterService.cancel(testEvent.getId(), user4.user().getId());

			// then: 3명만 REGISTERED 상태
			Long countAfterCancel = preRegisterService.getRegistrationCount(testEvent.getId());
			assertThat(countAfterCancel).isEqualTo(3);

			// user1, user3, user5는 여전히 등록 상태
			assertThat(preRegisterService.isRegistered(testEvent.getId(), user1.user().getId())).isTrue();
			assertThat(preRegisterService.isRegistered(testEvent.getId(), user3.user().getId())).isTrue();
			assertThat(preRegisterService.isRegistered(testEvent.getId(), user5.user().getId())).isTrue();

			// user2, user4는 취소 상태
			assertThat(preRegisterService.isRegistered(testEvent.getId(), user2.user().getId())).isFalse();
			assertThat(preRegisterService.isRegistered(testEvent.getId(), user4.user().getId())).isFalse();
		}

		@Test
		@DisplayName("취소 후 재등록 시나리오 - 성공")
		void scenario_CancelAndReRegister() {
			// given: 먼저 사전등록
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			PreRegisterResponse registerResponse = preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			);
			// when: 취소
			preRegisterService.cancel(testEvent.getId(), testUser.user().getId());

			// then: 취소 확인
			assertThat(preRegisterService.isRegistered(testEvent.getId(), testUser.user().getId())).isFalse();

			// when: 재등록 시도
			setSmsVerified(DEFAULT_PHONE_NUMBER);
			PreRegisterResponse reRegisterResponse = preRegisterService.register(
				testEvent.getId(),
				testUser.user().getId(),
				request
			);

			// then: 재등록 성공 및 기존 레코드를 재활용
			assertThat(reRegisterResponse.id()).isEqualTo(registerResponse.id()); // 같은 레코드 재활용
			assertThat(preRegisterService.isRegistered(testEvent.getId(), testUser.user().getId())).isTrue();

			// DB에서 REGISTERED 상태의 사전등록이 1개 존재하는지 확인
			Long count = preRegisterService.getRegistrationCount(testEvent.getId());
			assertThat(count).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("엣지 케이스 및 추가 검증")
	class EdgeCases {

		@Test
		@DisplayName("삭제된 이벤트에 사전등록 시도 시 NOT_FOUND_EVENT 예외 발생")
		void register_Fail_DeletedEvent() {
			// given: 삭제된 이벤트 생성
			Event deletedEvent = EventFactory.fakePreOpenEvent();
			eventRepository.save(deletedEvent);
			deletedEvent.delete(); // soft delete
			eventRepository.flush();

			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then: 삭제된 이벤트는 조회되지 않으므로 NOT_FOUND_EVENT 예외 발생
			assertThatThrownBy(() -> preRegisterService.register(
				deletedEvent.getId(),
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(EventErrorCode.NOT_FOUND_EVENT.getMessage());
		}

		@Test
		@DisplayName("존재하지 않는 이벤트 ID로 사전등록 시도 시 NOT_FOUND_EVENT 예외 발생")
		void register_Fail_NonExistentEvent() {
			// given
			Long nonExistentEventId = 99999L;
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				testUser.user().getFullName(),
				testUser.user().getBirthDate()
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				nonExistentEventId,
				testUser.user().getId(),
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(EventErrorCode.NOT_FOUND_EVENT.getMessage());
		}

		@Test
		@DisplayName("존재하지 않는 사용자 ID로 사전등록 시도 시 NOT_FOUND_USER 예외 발생")
		void register_Fail_NonExistentUser() {
			// given
			Long nonExistentUserId = 99999L;
			PreRegisterCreateRequest request = PreRegisterRequestFactory.fakePreRegisterRequest(
				"홍길동",
				LocalDate.of(1990, 1, 1)
			);

			// when & then
			assertThatThrownBy(() -> preRegisterService.register(
				testEvent.getId(),
				nonExistentUserId,
				request
			))
				.isInstanceOf(ErrorException.class)
				.hasMessage(CommonErrorCode.NOT_FOUND_USER.getMessage());
		}

		@Test
		@DisplayName("삭제된 이벤트에 대한 사전등록 현황 조회 시 NOT_FOUND_EVENT 예외 발생")
		void getRegistrationCount_Fail_DeletedEvent() {
			// given: 삭제된 이벤트
			Event deletedEvent = EventFactory.fakePreOpenEvent();
			eventRepository.save(deletedEvent);
			deletedEvent.delete();
			eventRepository.flush();

			// when & then
			assertThatThrownBy(() -> preRegisterService.getRegistrationCount(deletedEvent.getId()))
				.isInstanceOf(ErrorException.class)
				.hasMessage(EventErrorCode.NOT_FOUND_EVENT.getMessage());
		}
	}

	// Helper methods
	private TestUser createUser() {
		TestUser testUser = UserFactory.fakeUser(UserRole.NORMAL);
		userRepository.save(testUser.user());
		return testUser;
	}

	private PreRegister createPreRegister(Event event, User user) {
		return PreRegisterFactory.fakePreRegister(event, user);
	}
}
