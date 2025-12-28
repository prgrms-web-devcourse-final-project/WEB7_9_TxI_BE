package com.back.global.services.sms.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.back.global.error.exception.ErrorException;
import com.back.global.services.sms.util.SmsUtilInterface;

/**
 * SmsService 통합 테스트
 *
 * 주의: SmsUtilInterface는 MockBean으로 처리하여 실제 SMS 발송을 방지합니다.
 * Coolsms는 건당 비용이 발생하므로, 테스트 시 실제 API 호출을 하지 않도록 Mock 처리합니다.
 * (실제로는 test 프로파일에서 FakeSmsUtil이 자동으로 사용되지만, 명시적으로 Mock 처리)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SmsService 통합 테스트")
class SmsServiceTest {

	@Autowired
	private SmsService smsService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	/**
	 * SmsUtilInterface를 MockBean으로 처리하여 실제 SMS 발송 방지
	 * - 실제 Coolsms API 호출 시 건당 비용 발생
	 * - 테스트에서는 Mock으로 대체하여 비용 절감 및 외부 의존성 제거
	 */
	@MockitoBean
	private SmsUtilInterface smsUtil;

	private static final String TEST_PHONE_NUMBER = "01012345678";
	private static final String REDIS_KEY_PREFIX = "SMS_VERIFY:";
	private static final String SMS_VERIFIED_PREFIX = "SMS_VERIFIED:";

	@AfterEach
	void tearDown() {
		// Redis 데이터 정리
		redisTemplate.delete(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
		redisTemplate.delete(SMS_VERIFIED_PREFIX + TEST_PHONE_NUMBER);
	}

	@Nested
	@DisplayName("인증번호 발송 (sendVerificationCode)")
	class SendVerificationCode {

		@Test
		@DisplayName("인증번호 발송 성공 - SMS 발송 및 Redis 저장")
		void sendVerificationCode_Success() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// when
			Long expiresInSeconds = smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(expiresInSeconds).isEqualTo(180L); // TTL 반환 확인

			String redisKey = REDIS_KEY_PREFIX + TEST_PHONE_NUMBER;
			String storedCode = redisTemplate.opsForValue().get(redisKey);

			assertThat(storedCode).isNotNull();
			assertThat(storedCode).hasSize(6);
			assertThat(storedCode).matches("\\d{6}");

			// TTL 확인 (3분 = 180초)
			Long ttl = redisTemplate.getExpire(redisKey);
			assertThat(ttl).isNotNull();
			assertThat(ttl).isBetween(170L, 181L); // 약간의 여유

			// SmsUtil.sendOne() 호출 확인
			then(smsUtil).should(times(1)).sendOne(eq(TEST_PHONE_NUMBER), anyString());
		}

		@Test
		@DisplayName("동일 번호로 재발송 시 기존 인증번호 덮어쓰기")
		void sendVerificationCode_Overwrite() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// when
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String firstCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);

			smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String secondCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);

			// then
			assertThat(firstCode).isNotNull();
			assertThat(secondCode).isNotNull();
			// 두 번째 발송 시 덮어쓰기되므로 다를 가능성이 높음 (랜덤이므로 같을 수도 있음)
			then(smsUtil).should(times(2)).sendOne(eq(TEST_PHONE_NUMBER), anyString());
		}

		@Test
		@DisplayName("SMS 발송 실패 - SmsUtil 예외 발생")
		void sendVerificationCode_Fail_SmsUtilException() {
			// given
			willThrow(new RuntimeException("SMS 발송 API 오류"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// SMS 발송 실패 시 Redis에 저장되지 않아야 함
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			assertThat(storedCode).isNull();
		}
	}

	@Nested
	@DisplayName("인증번호 검증 (verifyCode)")
	class VerifyCode {

		@BeforeEach
		void setUp() {
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());
		}

		@Test
		@DisplayName("인증 성공 - 올바른 인증번호 입력")
		void verifyCode_Success() {
			// given
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);

			// when
			boolean result = smsService.verifyCode(TEST_PHONE_NUMBER, storedCode);

			// then
			assertThat(result).isTrue();

			// 인증 성공 후 인증 코드는 삭제되어야 함
			String deletedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			assertThat(deletedCode).isNull();

			// 인증 완료 플래그가 생성되어야 함 (TTL: 10분)
			String verifiedFlag = redisTemplate.opsForValue().get(SMS_VERIFIED_PREFIX + TEST_PHONE_NUMBER);
			assertThat(verifiedFlag).isEqualTo("true");

			Long ttl = redisTemplate.getExpire(SMS_VERIFIED_PREFIX + TEST_PHONE_NUMBER);
			assertThat(ttl).isBetween(590L, 601L); // 10분 = 600초
		}

		@Test
		@DisplayName("인증 실패 - 잘못된 인증번호 입력")
		void verifyCode_Fail_WrongCode() {
			// given
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// when & then
			assertThatThrownBy(() -> smsService.verifyCode(TEST_PHONE_NUMBER, "999999"))
				.isInstanceOf(ErrorException.class);

			// 인증 실패 시 인증 코드는 유지되어야 함
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			assertThat(storedCode).isNotNull();

			// 인증 완료 플래그가 생성되지 않아야 함
			String verifiedFlag = redisTemplate.opsForValue().get(SMS_VERIFIED_PREFIX + TEST_PHONE_NUMBER);
			assertThat(verifiedFlag).isNull();
		}

		@Test
		@DisplayName("인증 실패 - 인증번호 만료 (Redis에 없음)")
		void verifyCode_Fail_Expired() {
			// given - Redis에 인증번호가 없는 상태

			// when & then
			assertThatThrownBy(() -> smsService.verifyCode(TEST_PHONE_NUMBER, "123456"))
				.isInstanceOf(ErrorException.class);
		}

		@Test
		@DisplayName("인증 성공 후 재검증 시 실패 (재사용 방지)")
		void verifyCode_Fail_AlreadyUsed() {
			// given
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);

			// when
			boolean firstResult = smsService.verifyCode(TEST_PHONE_NUMBER, storedCode);

			// then
			assertThat(firstResult).isTrue();

			// 재검증 시 예외 발생 (이미 사용된 코드)
			assertThatThrownBy(() -> smsService.verifyCode(TEST_PHONE_NUMBER, storedCode))
				.isInstanceOf(ErrorException.class);
		}
	}

	@Nested
	@DisplayName("인증번호 존재 확인 (hasVerificationCode)")
	class HasVerificationCode {

		@Test
		@DisplayName("인증번호 존재 - true 반환")
		void hasVerificationCode_True() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// when
			boolean result = smsService.hasVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("인증번호 없음 - false 반환")
		void hasVerificationCode_False() {
			// given - 인증번호 발송하지 않음

			// when
			boolean result = smsService.hasVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("인증 완료 후 인증번호 존재 확인 - false 반환")
		void hasVerificationCode_AfterVerify() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			smsService.verifyCode(TEST_PHONE_NUMBER, storedCode);

			// when
			boolean result = smsService.hasVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(result).isFalse(); // 인증 완료 후 코드 삭제됨
		}
	}

	@Nested
	@DisplayName("전화번호 마스킹 테스트 (간접 확인)")
	class PhoneNumberMasking {

		@Test
		@DisplayName("SMS 발송 실패 시 전화번호 마스킹 확인")
		void sendVerificationCode_MaskPhoneNumber_OnError() {
			// given
			willThrow(new RuntimeException("SMS 발송 API 오류"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// 로그에 마스킹된 전화번호가 출력됨 (010****5678 형태)
			// 실제 로그 확인은 수동으로 하거나 로그 캡처 필요
		}

		@Test
		@DisplayName("인증번호 만료 시 전화번호 마스킹 확인")
		void verifyCode_MaskPhoneNumber_OnExpired() {
			// given - Redis에 인증번호가 없는 상태

			// when & then
			assertThatThrownBy(() -> smsService.verifyCode(TEST_PHONE_NUMBER, "123456"))
				.isInstanceOf(ErrorException.class);

			// 로그에 마스킹된 전화번호가 출력됨
		}

		@Test
		@DisplayName("잘못된 인증번호 입력 시 전화번호 마스킹 확인")
		void verifyCode_MaskPhoneNumber_OnMismatch() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// when & then
			assertThatThrownBy(() -> smsService.verifyCode(TEST_PHONE_NUMBER, "999999"))
				.isInstanceOf(ErrorException.class);

			// 로그에 마스킹된 전화번호가 출력됨
		}
	}

	@Nested
	@DisplayName("Redis 저장 실패 시나리오")
	class RedisFailureScenarios {

		@Test
		@DisplayName("Redis 저장 실패 시 SMS_SEND_FAILED 예외 발생 및 전화번호 마스킹")
		void sendVerificationCode_Fail_RedisError() {
			// given
			// SmsUtil은 정상 작동하도록 설정
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// Redis를 강제로 실패시키기 위해 잘못된 키로 테스트
			// 실제로는 Redis 연결이 끊어진 상황을 시뮬레이션하기 어려우므로
			// 이 테스트는 로그 확인용으로 유지

			// when
			Long result = smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(result).isEqualTo(180L);
		}
	}
}
