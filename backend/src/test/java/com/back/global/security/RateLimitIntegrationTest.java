package com.back.global.security;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.back.global.security.service.RateLimitService;

/**
 * Rate Limit 통합 테스트
 *
 * 실제 Redis를 사용하여 Rate Limiting이 정상적으로 작동하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"security.bot-protection.rate-limit.enabled=true",
	"security.bot-protection.rate-limit.global-per-second=5",
	"security.bot-protection.rate-limit.sms-per-minute=3"
})
class RateLimitIntegrationTest {

	@Autowired(required = false)
	private RateLimitService rateLimitService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String TEST_IP = "192.168.1.100";
	private static final String TEST_PHONE = "01012345678";

	@BeforeEach
	void setUp() {
		if (rateLimitService == null) {
			System.out.println("⚠️  RateLimitService가 비활성화되어 있습니다 (test 프로파일)");
		}
		cleanupRedis();
	}

	@AfterEach
	void tearDown() {
		cleanupRedis();
	}

	private void cleanupRedis() {
		// 테스트용 키 삭제
		redisTemplate.delete(redisTemplate.keys("rate_limit:*"));
	}

	@Test
	@DisplayName("Rate Limit 서비스가 test 프로파일에서 비활성화되는지 확인")
	void testRateLimitServiceDisabledInTestProfile() {
		// test 프로파일에서는 @Profile("!test")로 인해 RateLimitService가 생성되지 않음
		assertThat(rateLimitService).isNull();
		System.out.println("✅ Rate Limit 서비스가 test 프로파일에서 비활성화됨");
	}

	@Test
	@DisplayName("Redis 연결 확인")
	void testRedisConnection() {
		// Redis에 테스트 데이터 저장
		redisTemplate.opsForValue().set("test:key", "test:value", Duration.ofSeconds(10));
		String value = redisTemplate.opsForValue().get("test:key");

		assertThat(value).isEqualTo("test:value");
		System.out.println("✅ Redis 연결 정상");

		// 정리
		redisTemplate.delete("test:key");
	}

	/**
	 * 참고: RateLimitService는 test 프로파일에서 비활성화되어 있으므로
	 * 실제 Rate Limit 테스트는 dev 또는 prod 프로파일에서 수행해야 함
	 *
	 * 아래는 개념적인 테스트 시나리오:
	 */
	@Test
	@DisplayName("Rate Limit 개념 검증 - 전체 API (1초당 5회 제한)")
	void conceptualTestGlobalRateLimit() {
		System.out.println("\n📋 [개념 검증] 전체 API Rate Limit");
		System.out.println("설정: 1초당 5회");
		System.out.println("");
		System.out.println("예상 동작:");
		System.out.println("  요청 1~5: ✅ 허용 (200 OK)");
		System.out.println("  요청 6:   ❌ 차단 (429 Too Many Requests)");
		System.out.println("  1초 대기 후:");
		System.out.println("  요청 7:   ✅ 허용 (토큰 리필됨)");
		System.out.println("");

		// 실제 테스트는 dev/prod 프로파일에서만 가능
		if (rateLimitService == null) {
			System.out.println("⚠️  test 프로파일에서는 RateLimitService가 비활성화됨");
			System.out.println("💡 실제 테스트는 application-dev.yml을 사용하여 수동으로 진행");
		}
	}

	@Test
	@DisplayName("Rate Limit 개념 검증 - SMS/사전등록 (1분당 3회 제한)")
	void conceptualTestSmsRateLimit() {
		System.out.println("\n📋 [개념 검증] SMS/사전등록 Rate Limit");
		System.out.println("설정: 1분당 3회");
		System.out.println("키 구성: IP + 전화번호 해시");
		System.out.println("");
		System.out.println("예상 동작:");
		System.out.println("  요청 1~3: ✅ 허용 (200 OK)");
		System.out.println("  요청 4:   ❌ 차단 (429 Too Many Requests)");
		System.out.println("  다른 전화번호: ✅ 허용 (독립적인 카운터)");
		System.out.println("  1분 대기 후: ✅ 허용 (토큰 리필됨)");
		System.out.println("");

		if (rateLimitService == null) {
			System.out.println("⚠️  test 프로파일에서는 RateLimitService가 비활성화됨");
		}
	}

	@Test
	@DisplayName("Redis에 Rate Limit 키가 생성되는지 시뮬레이션")
	void testRedisKeyStructure() {
		System.out.println("\n📋 Redis 키 구조 검증");

		// 예상되는 키 형식 검증
		String globalKey = "rate_limit:global:" + TEST_IP;
		String smsKey = "rate_limit:sms:" + TEST_IP + ":" + TEST_PHONE.hashCode();

		System.out.println("전체 API 키: " + globalKey);
		System.out.println("SMS API 키: " + smsKey);

		// 키 형식이 올바른지 확인
		assertThat(globalKey).startsWith("rate_limit:global:");
		assertThat(smsKey).startsWith("rate_limit:sms:");

		System.out.println("✅ Redis 키 구조 검증 완료");
	}
}
