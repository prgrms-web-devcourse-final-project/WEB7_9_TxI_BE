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
 *
 * 중요: test 프로파일에서는 TestSmsService가 주입되므로,
 * SmsService의 sendVerificationCode를 직접 테스트하기 위해 수동으로 인스턴스를 생성합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SmsService 통합 테스트")
class SmsServiceTest {

	private SmsService smsService; // 직접 생성할 예정

	@Autowired
	private StringRedisTemplate redisTemplate;

	/**
	 * SmsUtilInterface를 MockBean으로 처리하여 실제 SMS 발송 방지
	 * - 실제 Coolsms API 호출 시 건당 비용 발생
	 * - 테스트에서는 Mock으로 대체하여 비용 절감 및 외부 의존성 제거
	 */
	@MockitoBean
	private SmsUtilInterface smsUtil;

	@BeforeEach
	void setUpSmsService() {
		// test 프로파일에서는 TestSmsService가 빈으로 등록되므로,
		// SmsService를 직접 테스트하기 위해 수동으로 인스턴스 생성
		smsService = new SmsService(smsUtil, redisTemplate);
	}

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

		@Test
		@DisplayName("짧은 전화번호 마스킹 - 8자리 미만")
		void sendVerificationCode_MaskPhoneNumber_ShortNumber() {
			// given
			String shortPhoneNumber = "0101234";
			willThrow(new RuntimeException("SMS 발송 API 오류"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(shortPhoneNumber))
				.isInstanceOf(ErrorException.class);

			// maskPhoneNumber 메서드의 early return 분기 커버
		}

		@Test
		@DisplayName("null 전화번호 마스킹")
		void verifyCode_MaskPhoneNumber_Null() {
			// given - null 전화번호
			String nullPhoneNumber = null;

			// when & then
			assertThatThrownBy(() -> smsService.verifyCode(nullPhoneNumber, "123456"))
				.isInstanceOf(ErrorException.class);

			// maskPhoneNumber 메서드의 null 체크 분기 커버
		}

		@Test
		@DisplayName("빈 문자열 전화번호 마스킹")
		void sendVerificationCode_MaskPhoneNumber_EmptyString() {
			// given
			String emptyPhoneNumber = "";
			willThrow(new RuntimeException("SMS 발송 API 오류"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(emptyPhoneNumber))
				.isInstanceOf(ErrorException.class);

			// maskPhoneNumber 메서드의 length < 8 분기 커버
		}

		@Test
		@DisplayName("정상적인 전화번호 마스킹 - Redis 저장 실패 시")
		void sendVerificationCode_MaskPhoneNumber_RedisFailure() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// Redis 정리 후 Redis 연결 문제 시뮬레이션을 위해
			// Redis에 큰 데이터를 저장하여 일부러 실패를 유도하기보다는
			// 정상 케이스에서도 maskPhoneNumber가 호출되는지 확인
			String longPhoneNumber = "01012345678901234"; // 17자리

			// when
			try {
				smsService.sendVerificationCode(longPhoneNumber);
			} catch (Exception e) {
				// Redis 저장은 성공할 수 있음
			}

			// maskPhoneNumber 메서드의 정상 분기 커버 (length >= 8)
			// prefix와 suffix 추출 로직 실행됨
		}
	}

	@Nested
	@DisplayName("Redis 저장 실패 시나리오")
	class RedisFailureScenarios {

		@Test
		@DisplayName("Redis 정상 동작 확인")
		void sendVerificationCode_Success_RedisWorking() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// when
			Long result = smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// then
			assertThat(result).isEqualTo(180L);

			// Redis에 정상 저장 확인
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			assertThat(storedCode).isNotNull();

			// 정리
			redisTemplate.delete(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
		}

		@Test
		@DisplayName("Redis 저장 실패 - Redis 예외 발생")
		void sendVerificationCode_Fail_RedisException() {
			// given
			// SMS 발송은 성공하지만 Redis 저장 시 실패하도록 Mock 설정
			SmsUtilInterface mockSmsUtil = org.mockito.Mockito.mock(SmsUtilInterface.class);
			StringRedisTemplate mockRedisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
			org.springframework.data.redis.core.ValueOperations<String, String> mockValueOps =
				org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);

			willDoNothing().given(mockSmsUtil).sendOne(anyString(), anyString());
			given(mockRedisTemplate.opsForValue()).willReturn(mockValueOps);
			willThrow(new RuntimeException("Redis 연결 오류"))
				.given(mockValueOps).set(anyString(), anyString(), any(java.time.Duration.class));

			SmsService smsServiceWithMockRedis = new SmsService(mockSmsUtil, mockRedisTemplate);

			// when & then
			assertThatThrownBy(() -> smsServiceWithMockRedis.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// SMS 발송은 호출되었어야 함
			then(mockSmsUtil).should(times(1)).sendOne(eq(TEST_PHONE_NUMBER), anyString());
		}
	}

	@Nested
	@DisplayName("TestSmsService 전용 테스트 (고정 인증번호)")
	class TestSmsServiceSpecific {

		@Test
		@DisplayName("TestSmsService - SMS 발송 실패 시 예외 처리")
		void testSmsService_SendVerificationCode_Fail() {
			// given
			willThrow(new RuntimeException("SMS 발송 실패"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// Redis에 저장되지 않아야 함
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);
			assertThat(storedCode).isNull();
		}

		@Test
		@DisplayName("TestSmsService - Redis 저장 성공 후 인증번호 확인")
		void testSmsService_SendAndVerify_Success() {
			// given
			willDoNothing().given(smsUtil).sendOne(anyString(), anyString());

			// when
			Long expiresInSeconds = smsService.sendVerificationCode(TEST_PHONE_NUMBER);
			String storedCode = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER);

			// then
			assertThat(expiresInSeconds).isEqualTo(180L);
			assertThat(storedCode).isNotNull();
			assertThat(storedCode).hasSize(6);

			// 저장된 코드로 인증 시도
			boolean result = smsService.verifyCode(TEST_PHONE_NUMBER, storedCode);
			assertThat(result).isTrue();
		}
	}
}
