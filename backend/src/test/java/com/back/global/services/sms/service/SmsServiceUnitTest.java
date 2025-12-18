package com.back.global.services.sms.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.back.global.error.exception.ErrorException;
import com.back.global.services.sms.util.SmsUtilInterface;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsService 단위 테스트 (실제 SmsService 메서드 테스트)")
class SmsServiceUnitTest {

	@Mock
	private SmsUtilInterface smsUtil;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private SmsService smsService;

	private static final String TEST_PHONE_NUMBER = "01012345678";
	private static final String REDIS_KEY_PREFIX = "SMS_VERIFY:";

	@Nested
	@DisplayName("인증번호 발송 (sendVerificationCode) - SmsService 직접 테스트")
	class SendVerificationCode {

		@Test
		@DisplayName("인증번호 발송 성공 - SmsService.sendVerificationCode() 직접 호출")
		void sendVerificationCode_Success() {
			// given
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
			willDoNothing().given(smsUtil).sendOne(eq(TEST_PHONE_NUMBER), anyString());

			// when
			smsService.sendVerificationCode(TEST_PHONE_NUMBER);

			// then
			then(smsUtil).should(times(1)).sendOne(eq(TEST_PHONE_NUMBER), anyString());
			then(valueOperations).should(times(1))
				.set(eq(REDIS_KEY_PREFIX + TEST_PHONE_NUMBER), anyString(), any(Duration.class));
		}

		@Test
		@DisplayName("SMS 발송 실패 - SmsUtil 예외 발생 시 ErrorException throw")
		void sendVerificationCode_Fail_SmsUtilException() {
			// given
			willThrow(new RuntimeException("SMS 발송 API 오류"))
				.given(smsUtil).sendOne(anyString(), anyString());

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// SMS 발송 실패 시 Redis 저장이 호출되지 않아야 함
			then(valueOperations).should(never()).set(anyString(), anyString(), any(Duration.class));
		}

		@Test
		@DisplayName("Redis 저장 실패 - Redis 예외 발생 시 ErrorException throw")
		void sendVerificationCode_Fail_RedisException() {
			// given
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
			willDoNothing().given(smsUtil).sendOne(eq(TEST_PHONE_NUMBER), anyString());
			willThrow(new RuntimeException("Redis 저장 오류"))
				.given(valueOperations).set(anyString(), anyString(), any(Duration.class));

			// when & then
			assertThatThrownBy(() -> smsService.sendVerificationCode(TEST_PHONE_NUMBER))
				.isInstanceOf(ErrorException.class);

			// SMS는 발송되었어야 함
			then(smsUtil).should(times(1)).sendOne(eq(TEST_PHONE_NUMBER), anyString());
		}
	}
}
