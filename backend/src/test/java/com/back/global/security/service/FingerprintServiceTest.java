package com.back.global.security.service;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.global.properties.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("FingerprintService 단위 테스트")
class FingerprintServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	private FingerprintService fingerprintService;
	private SecurityProperties securityProperties;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		// SecurityProperties 설정
		securityProperties = new SecurityProperties();
		SecurityProperties.Fingerprint fingerprintProps = new SecurityProperties.Fingerprint();
		fingerprintProps.setEnabled(true);
		fingerprintProps.setMinAttempts(5);
		fingerprintProps.setMaxAttempts(10);
		fingerprintProps.setFailureRateThreshold(0.8);
		fingerprintProps.setTtlSeconds(86400L);
		securityProperties.setFingerprint(fingerprintProps);

		// ObjectMapper 생성
		objectMapper = new ObjectMapper();

		// Mock 설정 - lenient로 변경하여 unnecessary stubbing 에러 방지
		lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);

		// FingerprintService 생성
		fingerprintService = new FingerprintService(redisTemplate, securityProperties, objectMapper);
	}

	@Nested
	@DisplayName("Fingerprint 검증 (validateFingerprint)")
	class ValidateFingerprint {

		@Test
		@DisplayName("시도 횟수 < 5회면 항상 허용")
		void validateFingerprint_LessThanMinAttempts() {
			// given
			String visitorId = "new-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("3");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("2");

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("시도 횟수 >= 5회, 실패율 < 80%이면 허용")
		void validateFingerprint_AllowedWhenBelowThreshold() {
			// given
			String visitorId = "good-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("7");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("2"); // 실패율 약 28.6%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("시도 횟수 >= 5회, 실패율 >= 80%이면 차단")
		void validateFingerprint_BlockedWhenExceedsThreshold() {
			// given
			String visitorId = "suspicious-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("5");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("4"); // 실패율 80%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("시도 기록이 없으면 허용 (신규 사용자)")
		void validateFingerprint_AllowedWhenNoRecord() {
			// given
			String visitorId = "first-time-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn(null);

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("시도 횟수 10회, 실패율 정확히 80%이면 차단")
		void validateFingerprint_ExactlyThreshold() {
			// given
			String visitorId = "edge-case-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("10");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("8"); // 실패율 정확히 80%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("총 시도 횟수 >= 10회이면 실패율 관계없이 차단 (성공 폭탄 공격 방어)")
		void validateFingerprint_BlockedWhenMaxAttemptsExceeded() {
			// given
			String visitorId = "spam-attacker";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("10");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("0"); // 실패율 0%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("총 시도 횟수 9회이면 실패율 0%일 때 허용")
		void validateFingerprint_AllowedWhenBelowMaxAttempts() {
			// given
			String visitorId = "normal-user";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("9");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("0"); // 실패율 0%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("총 시도 횟수 15회 성공이면 차단 (최대 시도 횟수 초과)")
		void validateFingerprint_BlockedWhen15SuccessfulAttempts() {
			// given
			String visitorId = "success-bomber";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("15");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("0"); // 모두 성공

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}
	}

	@Nested
	@DisplayName("시도 기록 (recordAttempt)")
	class RecordAttempt {

		@Test
		@DisplayName("성공 시도 기록 - 신규 사용자")
		void recordAttempt_Success_NewUser() {
			// given
			String visitorId = "new-user";
			Long eventId = 1L;
			String action = "sms_send";
			String expectedKey = "fingerprint:" + visitorId + ":" + eventId + ":" + action;
			when(hashOperations.increment(eq(expectedKey), eq("totalAttempts"), eq(1L))).thenReturn(1L);
			when(hashOperations.increment(eq(expectedKey), eq("successCount"), eq(1L))).thenReturn(1L);

			// when
			fingerprintService.recordAttempt(visitorId, eventId, action, true);

			// then
			verify(hashOperations).increment(eq(expectedKey), eq("totalAttempts"), eq(1L));
			verify(hashOperations).increment(eq(expectedKey), eq("successCount"), eq(1L));
			verify(hashOperations, never()).increment(eq(expectedKey), eq("failedAttempts"), anyLong());
			verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofSeconds(86400L)));
		}

		@Test
		@DisplayName("실패 시도 기록 - 신규 사용자")
		void recordAttempt_Failure_NewUser() {
			// given
			String visitorId = "new-failing-user";
			Long eventId = 1L;
			String action = "sms_send";
			String expectedKey = "fingerprint:" + visitorId + ":" + eventId + ":" + action;
			when(hashOperations.increment(eq(expectedKey), eq("totalAttempts"), eq(1L))).thenReturn(1L);
			when(hashOperations.increment(eq(expectedKey), eq("failedAttempts"), eq(1L))).thenReturn(1L);

			// when
			fingerprintService.recordAttempt(visitorId, eventId, action, false);

			// then
			verify(hashOperations).increment(eq(expectedKey), eq("totalAttempts"), eq(1L));
			verify(hashOperations).increment(eq(expectedKey), eq("failedAttempts"), eq(1L));
			verify(hashOperations, never()).increment(eq(expectedKey), eq("successCount"), anyLong());
			verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofSeconds(86400L)));
		}

		@Test
		@DisplayName("성공 시도 기록 - 기존 사용자")
		void recordAttempt_Success_ExistingUser() {
			// given
			String visitorId = "existing-user";
			Long eventId = 1L;
			String action = "sms_send";
			String expectedKey = "fingerprint:" + visitorId + ":" + eventId + ":" + action;
			when(hashOperations.increment(eq(expectedKey), eq("totalAttempts"), eq(1L))).thenReturn(6L);
			when(hashOperations.increment(eq(expectedKey), eq("successCount"), eq(1L))).thenReturn(4L);

			// when
			fingerprintService.recordAttempt(visitorId, eventId, action, true);

			// then
			verify(hashOperations).increment(eq(expectedKey), eq("totalAttempts"), eq(1L));
			verify(hashOperations).increment(eq(expectedKey), eq("successCount"), eq(1L));
			verify(hashOperations, never()).increment(eq(expectedKey), eq("failedAttempts"), anyLong());
		}

		@Test
		@DisplayName("실패 시도 기록 - 기존 사용자")
		void recordAttempt_Failure_ExistingUser() {
			// given
			String visitorId = "existing-failing-user";
			Long eventId = 1L;
			String action = "sms_send";
			String expectedKey = "fingerprint:" + visitorId + ":" + eventId + ":" + action;
			when(hashOperations.increment(eq(expectedKey), eq("totalAttempts"), eq(1L))).thenReturn(6L);
			when(hashOperations.increment(eq(expectedKey), eq("failedAttempts"), eq(1L))).thenReturn(3L);

			// when
			fingerprintService.recordAttempt(visitorId, eventId, action, false);

			// then
			verify(hashOperations).increment(eq(expectedKey), eq("totalAttempts"), eq(1L));
			verify(hashOperations).increment(eq(expectedKey), eq("failedAttempts"), eq(1L));
			verify(hashOperations, never()).increment(eq(expectedKey), eq("successCount"), anyLong());
		}

		@Test
		@DisplayName("TTL 설정 확인 - 86400초 (24시간)")
		void recordAttempt_SetsTTL() {
			// given
			String visitorId = "ttl-test-user";
			Long eventId = 1L;
			String action = "sms_send";
			String expectedKey = "fingerprint:" + visitorId + ":" + eventId + ":" + action;
			when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(1L);

			// when
			fingerprintService.recordAttempt(visitorId, eventId, action, true);

			// then
			verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofSeconds(86400L)));
		}
	}

	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCases {

		@Test
		@DisplayName("visitorId가 null이면 검증 실패하지 않음")
		void validateFingerprint_NullVisitorId() {
			// when & then - NPE가 발생하지 않아야 함
			assertThatCode(() -> fingerprintService.validateFingerprint(null, 1L, "sms_send"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("visitorId가 빈 문자열이면 검증 실패하지 않음")
		void validateFingerprint_EmptyVisitorId() {
			// when & then
			assertThatCode(() -> fingerprintService.validateFingerprint("", 1L, "sms_send"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("매우 높은 실패율 (100%) - 차단")
		void validateFingerprint_VeryHighFailureRate() {
			// given
			String visitorId = "all-failed-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("10");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("10"); // 실패율 100%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("실패율 0%, 총 시도 < 10회 - 허용")
		void validateFingerprint_ZeroFailureRate() {
			// given
			String visitorId = "perfect-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn("8");
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn("0"); // 실패율 0%

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("Fingerprint 기능 비활성화 시 항상 허용")
		void validateFingerprint_Disabled() {
			// given
			securityProperties.getFingerprint().setEnabled(false);
			FingerprintService disabledService = new FingerprintService(redisTemplate, securityProperties, objectMapper);

			// when
			boolean allowed = disabledService.validateFingerprint("any-visitor", 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("Redis에서 Integer 타입 반환 시 정상 처리")
		void validateFingerprint_IntegerTypeFromRedis() {
			// given
			String visitorId = "integer-type-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn(7); // Integer 타입
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn(2);

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("Redis에서 Long 타입 반환 시 정상 처리")
		void validateFingerprint_LongTypeFromRedis() {
			// given
			String visitorId = "long-type-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn(10L); // Long 타입
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn(8L);

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("Redis에서 알 수 없는 타입 반환 시 0으로 처리")
		void validateFingerprint_UnknownTypeFromRedis() {
			// given
			String visitorId = "unknown-type-visitor";
			when(hashOperations.get(anyString(), eq("totalAttempts"))).thenReturn(new Object());
			when(hashOperations.get(anyString(), eq("failedAttempts"))).thenReturn(new Object());

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then: 시도 횟수가 0이므로 허용
			assertThat(allowed).isTrue();
		}

		@Test
		@DisplayName("Redis 예외 발생 시 요청 허용 (가용성 우선)")
		void validateFingerprint_RedisException() {
			// given
			String visitorId = "redis-error-visitor";
			when(hashOperations.get(anyString(), anyString())).thenThrow(new RuntimeException("Redis connection error"));

			// when
			boolean allowed = fingerprintService.validateFingerprint(visitorId, 1L, "sms_send");

			// then: 예외 발생해도 요청 허용
			assertThat(allowed).isTrue();
		}
	}

	@Nested
	@DisplayName("recordAttempt 엣지 케이스")
	class RecordAttemptEdgeCases {

		@Test
		@DisplayName("Fingerprint 기능 비활성화 시 기록하지 않음")
		void recordAttempt_Disabled() {
			// given
			securityProperties.getFingerprint().setEnabled(false);
			FingerprintService disabledService = new FingerprintService(redisTemplate, securityProperties, objectMapper);

			// when
			disabledService.recordAttempt("any-visitor", 1L, "sms_send", true);

			// then: Redis 호출 없음
			verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
		}

		@Test
		@DisplayName("null visitorId는 기록하지 않음")
		void recordAttempt_NullVisitorId() {
			// when
			fingerprintService.recordAttempt(null, 1L, "sms_send", true);

			// then: Redis 호출 없음
			verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
		}

		@Test
		@DisplayName("빈 문자열 visitorId는 기록하지 않음")
		void recordAttempt_EmptyVisitorId() {
			// when
			fingerprintService.recordAttempt("", 1L, "sms_send", true);

			// then: Redis 호출 없음
			verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
		}

		@Test
		@DisplayName("Redis 예외 발생 시 에러 로그만 남기고 계속 진행")
		void recordAttempt_RedisException() {
			// given
			String visitorId = "error-visitor";
			when(hashOperations.increment(anyString(), anyString(), anyLong()))
				.thenThrow(new RuntimeException("Redis error"));

			// when & then: 예외 발생하지 않음
			assertThatCode(() -> fingerprintService.recordAttempt(visitorId, 1L, "sms_send", true))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("resetFingerprint")
	class ResetFingerprint {

		@Test
		@DisplayName("Fingerprint 데이터 초기화 성공")
		void resetFingerprint_Success() {
			// given
			String visitorId = "reset-visitor";

			// when
			fingerprintService.resetFingerprint(visitorId);

			// then
			verify(redisTemplate).delete(eq("fingerprint:" + visitorId));
		}

		@Test
		@DisplayName("null visitorId는 초기화하지 않음")
		void resetFingerprint_NullVisitorId() {
			// when
			fingerprintService.resetFingerprint(null);

			// then: Redis 호출 없음
			verify(redisTemplate, never()).delete(anyString());
		}

		@Test
		@DisplayName("빈 문자열 visitorId는 초기화하지 않음")
		void resetFingerprint_EmptyVisitorId() {
			// when
			fingerprintService.resetFingerprint("");

			// then: Redis 호출 없음
			verify(redisTemplate, never()).delete(anyString());
		}
	}
}
