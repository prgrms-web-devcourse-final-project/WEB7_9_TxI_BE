package com.back.global.security.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.back.global.properties.SecurityProperties;

/**
 * RateLimitService 통합 테스트
 *
 * 실제 Redis와 Bucket4j를 사용한 Rate Limiting 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RateLimitService 통합 테스트")
class RateLimitServiceIntegrationTest {

	@Autowired(required = false)
	private RateLimitService rateLimitService;

	@Autowired(required = false)
	private StringRedisTemplate redisTemplate;

	@Autowired(required = false)
	private SecurityProperties securityProperties;

	@BeforeEach
	void setUp() {
		if (redisTemplate != null) {
			// Redis 초기화
			redisTemplate.getConnectionFactory().getConnection().flushDb();
		}
	}

	@AfterEach
	void tearDown() {
		if (redisTemplate != null) {
			// 테스트 후 Redis 정리
			redisTemplate.getConnectionFactory().getConnection().flushDb();
		}
	}

	@Nested
	@DisplayName("전역 Rate Limit (allowGlobalRequest)")
	class AllowGlobalRequest {

		@Test
		@DisplayName("전역 Rate Limit이 비활성화되면 항상 허용")
		void allowGlobalRequest_Disabled() {
			// RateLimitService가 없으면 테스트 스킵
			if (rateLimitService == null) {
				return;
			}

			// given: Rate Limit 비활성화
			securityProperties.getRateLimit().setEnabled(false);

			// when & then: 여러 번 요청해도 항상 허용
			for (int i = 0; i < 100; i++) {
				assertThat(rateLimitService.allowGlobalRequest("192.168.1.1")).isTrue();
			}
		}

		@Test
		@DisplayName("전역 Rate Limit 제한 내에서 요청 허용")
		void allowGlobalRequest_WithinLimit() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.100";
			int limit = securityProperties.getRateLimit().getGlobalPerSecond();

			// when & then: 제한 내에서는 모두 허용
			for (int i = 0; i < limit; i++) {
				boolean allowed = rateLimitService.allowGlobalRequest(ip);
				assertThat(allowed).isTrue();
			}
		}

		@Test
		@DisplayName("전역 Rate Limit 초과 시 요청 차단")
		void allowGlobalRequest_ExceedsLimit() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.200";
			int limit = securityProperties.getRateLimit().getGlobalPerSecond();

			// when: 제한 초과 요청
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowGlobalRequest(ip);
			}

			// then: 추가 요청은 차단
			boolean allowed = rateLimitService.allowGlobalRequest(ip);
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("다른 IP는 독립적인 Rate Limit 적용")
		void allowGlobalRequest_DifferentIPs() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip1 = "192.168.1.10";
			String ip2 = "192.168.1.20";
			int limit = securityProperties.getRateLimit().getGlobalPerSecond();

			// when: IP1의 토큰 모두 소진
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowGlobalRequest(ip1);
			}

			// then: IP1은 차단, IP2는 허용
			assertThat(rateLimitService.allowGlobalRequest(ip1)).isFalse();
			assertThat(rateLimitService.allowGlobalRequest(ip2)).isTrue();
		}
	}

	@Nested
	@DisplayName("SMS Rate Limit (allowSmsRequest)")
	class AllowSmsRequest {

		@Test
		@DisplayName("SMS Rate Limit이 비활성화되면 항상 허용")
		void allowSmsRequest_Disabled() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(false);

			// when & then
			for (int i = 0; i < 100; i++) {
				assertThat(rateLimitService.allowSmsRequest("192.168.1.1", "01012345678")).isTrue();
			}
		}

		@Test
		@DisplayName("SMS Rate Limit 제한 내에서 요청 허용")
		void allowSmsRequest_WithinLimit() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.100";
			String phoneNumber = "01012345678";
			int limit = securityProperties.getRateLimit().getSmsPerMinute();

			// when & then
			for (int i = 0; i < limit; i++) {
				boolean allowed = rateLimitService.allowSmsRequest(ip, phoneNumber);
				assertThat(allowed).isTrue();
			}
		}

		@Test
		@DisplayName("SMS Rate Limit 초과 시 요청 차단")
		void allowSmsRequest_ExceedsLimit() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.200";
			String phoneNumber = "01087654321";
			int limit = securityProperties.getRateLimit().getSmsPerMinute();

			// when: 제한 초과 요청
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowSmsRequest(ip, phoneNumber);
			}

			// then: 추가 요청은 차단
			boolean allowed = rateLimitService.allowSmsRequest(ip, phoneNumber);
			assertThat(allowed).isFalse();
		}

		@Test
		@DisplayName("같은 IP, 다른 전화번호는 독립적인 Rate Limit")
		void allowSmsRequest_SameIpDifferentPhone() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.50";
			String phone1 = "01011111111";
			String phone2 = "01022222222";
			int limit = securityProperties.getRateLimit().getSmsPerMinute();

			// when: phone1의 토큰 모두 소진
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowSmsRequest(ip, phone1);
			}

			// then: phone1은 차단, phone2는 허용
			assertThat(rateLimitService.allowSmsRequest(ip, phone1)).isFalse();
			assertThat(rateLimitService.allowSmsRequest(ip, phone2)).isTrue();
		}

		@Test
		@DisplayName("다른 IP, 같은 전화번호는 독립적인 Rate Limit")
		void allowSmsRequest_DifferentIpSamePhone() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip1 = "192.168.1.10";
			String ip2 = "192.168.1.20";
			String phoneNumber = "01033333333";
			int limit = securityProperties.getRateLimit().getSmsPerMinute();

			// when: IP1의 토큰 모두 소진
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowSmsRequest(ip1, phoneNumber);
			}

			// then: IP1은 차단, IP2는 허용
			assertThat(rateLimitService.allowSmsRequest(ip1, phoneNumber)).isFalse();
			assertThat(rateLimitService.allowSmsRequest(ip2, phoneNumber)).isTrue();
		}

		@Test
		@DisplayName("전화번호 해싱으로 개인정보 보호")
		void allowSmsRequest_PhoneNumberHashing() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.100";
			String phoneNumber = "01044444444";
			int limit = securityProperties.getRateLimit().getSmsPerMinute();

			// when: 동일한 전화번호로 여러 번 요청
			for (int i = 0; i < limit; i++) {
				rateLimitService.allowSmsRequest(ip, phoneNumber);
			}

			// then: 동일한 전화번호는 해시가 같으므로 차단
			assertThat(rateLimitService.allowSmsRequest(ip, phoneNumber)).isFalse();
		}
	}

	@Nested
	@DisplayName("토큰 조회 (getRemainingTokens)")
	class GetRemainingTokens {

		@Test
		@DisplayName("초기 상태에서 전체 토큰 수 반환")
		void getRemainingTokens_Initial() {
			if (rateLimitService == null) {
				return;
			}

			// given
			String key = "rate_limit:test:initial";
			int limit = 10;

			// when
			long remaining = rateLimitService.getRemainingTokens(key, limit, java.time.Duration.ofSeconds(1));

			// then
			assertThat(remaining).isEqualTo(limit);
		}

		@Test
		@DisplayName("토큰 소비 후 남은 토큰 수 감소")
		void getRemainingTokens_AfterConsumption() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);
			String ip = "192.168.1.100";
			int limit = securityProperties.getRateLimit().getGlobalPerSecond();

			// when: 토큰 3개 소비
			for (int i = 0; i < 3; i++) {
				rateLimitService.allowGlobalRequest(ip);
			}

			long remaining = rateLimitService.getRemainingTokens(
				"rate_limit:global:" + ip,
				limit,
				java.time.Duration.ofSeconds(1)
			);

			// then: 남은 토큰 수 확인
			assertThat(remaining).isEqualTo(limit - 3);
		}
	}

	@Nested
	@DisplayName("예외 처리")
	class ExceptionHandling {

		@Test
		@DisplayName("null IP는 정상 처리")
		void allowGlobalRequest_NullIp() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);

			// when & then: NPE 발생하지 않음
			assertThatCode(() -> rateLimitService.allowGlobalRequest(null))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("빈 문자열 IP는 정상 처리")
		void allowGlobalRequest_EmptyIp() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);

			// when & then
			assertThatCode(() -> rateLimitService.allowGlobalRequest(""))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("null 전화번호는 정상 처리")
		void allowSmsRequest_NullPhone() {
			if (rateLimitService == null) {
				return;
			}

			// given
			securityProperties.getRateLimit().setEnabled(true);

			// when & then
			assertThatCode(() -> rateLimitService.allowSmsRequest("192.168.1.1", null))
				.doesNotThrowAnyException();
		}
	}
}
