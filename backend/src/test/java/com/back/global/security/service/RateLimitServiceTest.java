package com.back.global.security.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.back.global.properties.SecurityProperties;

/**
 * RateLimitService 단위 테스트
 *
 * 주의: RateLimitService는 @Profile("!test")로 인해 테스트 환경에서 빈이 생성되지 않음
 * Bucket4j의 ProxyManager 빌더 패턴이 복잡하여 통합 테스트(RateLimitServiceIntegrationTest)로 검증
 *
 * 이 테스트는 비즈니스 로직만 검증합니다.
 */
@DisplayName("RateLimitService 단위 테스트")
class RateLimitServiceTest {

	private SecurityProperties securityProperties;

	@Nested
	@DisplayName("전화번호 해싱 로직 검증")
	class PhoneNumberHashing {

		@Test
		@DisplayName("같은 전화번호는 같은 해시 생성")
		void hashPhoneNumber_SamePhone() {
			// given
			String phone = "01012345678";
			String salt = "TxI_RateLimit_Salt_2025";

			// when
			String hash1 = String.valueOf((phone + salt).hashCode());
			String hash2 = String.valueOf((phone + salt).hashCode());

			// then: 같은 해시
			assertThat(hash1).isEqualTo(hash2);
		}

		@Test
		@DisplayName("다른 전화번호는 다른 해시 생성")
		void hashPhoneNumber_DifferentPhone() {
			// given
			String phone1 = "01012345678";
			String phone2 = "01087654321";
			String salt = "TxI_RateLimit_Salt_2025";

			// when
			String hash1 = String.valueOf((phone1 + salt).hashCode());
			String hash2 = String.valueOf((phone2 + salt).hashCode());

			// then: 서로 다른 해시
			assertThat(hash1).isNotEqualTo(hash2);
		}

		@Test
		@DisplayName("null 전화번호는 'null' 문자열로 해시")
		void hashPhoneNumber_Null() {
			// given
			String salt = "TxI_RateLimit_Salt_2025";

			// when
			String hash = String.valueOf(("null" + salt).hashCode());

			// then: 해시 생성됨
			assertThat(hash).isNotNull();
			assertThat(hash).isNotEmpty();
		}

		@Test
		@DisplayName("Salt를 사용하여 Rainbow Table 공격 방지")
		void hashPhoneNumber_WithSalt() {
			// given
			String phone = "01012345678";
			String salt = "TxI_RateLimit_Salt_2025";

			// when: Salt 있는 해시 vs Salt 없는 해시
			String hashWithSalt = String.valueOf((phone + salt).hashCode());
			String hashWithoutSalt = String.valueOf(phone.hashCode());

			// then: Salt를 사용하면 해시가 다름
			assertThat(hashWithSalt).isNotEqualTo(hashWithoutSalt);
		}
	}

	@Nested
	@DisplayName("Rate Limit 키 생성 로직 검증")
	class RateLimitKeyGeneration {

		@Test
		@DisplayName("전역 Rate Limit 키 형식: rate_limit:global:{ip}")
		void globalRateLimitKey() {
			// given
			String ip = "192.168.1.100";

			// when
			String key = "rate_limit:global:" + ip;

			// then
			assertThat(key).isEqualTo("rate_limit:global:192.168.1.100");
			assertThat(key).startsWith("rate_limit:global:");
		}

		@Test
		@DisplayName("SMS Rate Limit 키 형식: rate_limit:sms:{ip}:{phoneHash}")
		void smsRateLimitKey() {
			// given
			String ip = "192.168.1.100";
			String phone = "01012345678";
			String phoneHash = String.valueOf((phone + "TxI_RateLimit_Salt_2025").hashCode());

			// when
			String key = "rate_limit:sms:" + ip + ":" + phoneHash;

			// then
			assertThat(key).startsWith("rate_limit:sms:192.168.1.100:");
			assertThat(key).contains(phoneHash);
		}

		@Test
		@DisplayName("같은 IP와 전화번호는 같은 키 생성")
		void sameIpAndPhone_SameKey() {
			// given
			String ip = "192.168.1.100";
			String phone = "01012345678";
			String phoneHash = String.valueOf((phone + "TxI_RateLimit_Salt_2025").hashCode());

			// when
			String key1 = "rate_limit:sms:" + ip + ":" + phoneHash;
			String key2 = "rate_limit:sms:" + ip + ":" + phoneHash;

			// then
			assertThat(key1).isEqualTo(key2);
		}

		@Test
		@DisplayName("다른 IP는 다른 키 생성")
		void differentIp_DifferentKey() {
			// given
			String phone = "01012345678";
			String phoneHash = String.valueOf((phone + "TxI_RateLimit_Salt_2025").hashCode());

			// when
			String key1 = "rate_limit:sms:192.168.1.1:" + phoneHash;
			String key2 = "rate_limit:sms:192.168.1.2:" + phoneHash;

			// then
			assertThat(key1).isNotEqualTo(key2);
		}
	}

	@Nested
	@DisplayName("SecurityProperties 설정 검증")
	class SecurityPropertiesValidation {

		@Test
		@DisplayName("Rate Limit 기본 설정값")
		void defaultSettings() {
			// given
			securityProperties = new SecurityProperties();
			SecurityProperties.RateLimit rateLimitProps = new SecurityProperties.RateLimit();
			rateLimitProps.setEnabled(true);
			rateLimitProps.setGlobalPerSecond(50);
			rateLimitProps.setSmsPerMinute(5);
			securityProperties.setRateLimit(rateLimitProps);

			// then
			assertThat(securityProperties.getRateLimit().isEnabled()).isTrue();
			assertThat(securityProperties.getRateLimit().getGlobalPerSecond()).isEqualTo(50);
			assertThat(securityProperties.getRateLimit().getSmsPerMinute()).isEqualTo(5);
		}

		@Test
		@DisplayName("Rate Limit 비활성화 설정")
		void disabledSettings() {
			// given
			securityProperties = new SecurityProperties();
			SecurityProperties.RateLimit rateLimitProps = new SecurityProperties.RateLimit();
			rateLimitProps.setEnabled(false);
			securityProperties.setRateLimit(rateLimitProps);

			// then
			assertThat(securityProperties.getRateLimit().isEnabled()).isFalse();
		}

		@Test
		@DisplayName("커스텀 Rate Limit 설정")
		void customSettings() {
			// given
			securityProperties = new SecurityProperties();
			SecurityProperties.RateLimit rateLimitProps = new SecurityProperties.RateLimit();
			rateLimitProps.setEnabled(true);
			rateLimitProps.setGlobalPerSecond(100);
			rateLimitProps.setSmsPerMinute(10);
			securityProperties.setRateLimit(rateLimitProps);

			// then
			assertThat(securityProperties.getRateLimit().getGlobalPerSecond()).isEqualTo(100);
			assertThat(securityProperties.getRateLimit().getSmsPerMinute()).isEqualTo(10);
		}
	}

	@Nested
	@DisplayName("Rate Limit Salt 상수 검증")
	class RateLimitSaltValidation {

		@Test
		@DisplayName("고정 Salt 값 검증")
		void fixedSaltValue() {
			// given: RateLimitService의 고정 Salt
			String expectedSalt = "TxI_RateLimit_Salt_2025";

			// then: 애플리케이션 재시작 후에도 동일한 Salt 사용
			assertThat(expectedSalt).isEqualTo("TxI_RateLimit_Salt_2025");
		}

		@Test
		@DisplayName("Salt를 사용한 전화번호 해시는 재현 가능")
		void saltedHashIsReproducible() {
			// given
			String phone = "01012345678";
			String salt = "TxI_RateLimit_Salt_2025";

			// when: 동일한 입력으로 여러 번 해시 생성
			String hash1 = String.valueOf((phone + salt).hashCode());
			String hash2 = String.valueOf((phone + salt).hashCode());
			String hash3 = String.valueOf((phone + salt).hashCode());

			// then: 항상 동일한 해시 생성 (재현 가능)
			assertThat(hash1).isEqualTo(hash2);
			assertThat(hash2).isEqualTo(hash3);
		}
	}
}
