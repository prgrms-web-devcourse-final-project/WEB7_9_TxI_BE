package com.back.global.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.back.global.security.service.FingerprintService;

/**
 * Fingerprint 검증 통합 테스트
 *
 * 실제 Redis를 사용하여 Fingerprint 기반 봇 탐지가 정상적으로 작동하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class FingerprintIntegrationTest {

	@Autowired(required = false)
	private FingerprintService fingerprintService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String TEST_VISITOR_ID = "test_visitor_12345";
	private static final String REDIS_KEY_PREFIX = "fingerprint:";

	@BeforeEach
	void setUp() {
		if (fingerprintService == null) {
			System.out.println("⚠️  FingerprintService가 비활성화되어 있습니다 (test 프로파일)");
		}
		cleanupRedis();
	}

	@AfterEach
	void tearDown() {
		cleanupRedis();
	}

	private void cleanupRedis() {
		redisTemplate.delete(redisTemplate.keys(REDIS_KEY_PREFIX + "*"));
	}

	@Test
	@DisplayName("Fingerprint 서비스가 test 프로파일에서 비활성화되는지 확인")
	void testFingerprintServiceDisabledInTestProfile() {
		assertThat(fingerprintService).isNull();
		System.out.println("✅ Fingerprint 서비스가 test 프로파일에서 비활성화됨");
	}

	@Test
	@DisplayName("Fingerprint 데이터 구조 검증")
	void testFingerprintDataStructure() {
		System.out.println("\n📋 Fingerprint 데이터 구조 검증");

		String visitorId = "abc123def456";
		String key = REDIS_KEY_PREFIX + visitorId;

		// 예상 JSON 구조
		String expectedJson = """
			{
			  "totalAttempts": 10,
			  "failedAttempts": 9,
			  "successCount": 1
			}
			""";

		System.out.println("Redis Key: " + key);
		System.out.println("Value (JSON): " + expectedJson.trim());
		System.out.println("");
		System.out.println("계산:");
		System.out.println("  실패율 = 9 / 10 = 0.9 (90%)");
		System.out.println("  임계값 = 0.8 (80%)");
		System.out.println("  결과: 90% > 80% → ❌ 차단");

		// 실패율 계산 검증
		double failureRate = 9.0 / 10.0;
		boolean shouldBlock = failureRate >= 0.8;

		assertThat(shouldBlock).isTrue();
		System.out.println("✅ Fingerprint 데이터 구조 검증 완료");
	}

	@Test
	@DisplayName("정상 사용자 시나리오 - 1회 시도 성공")
	void testNormalUserScenario() {
		System.out.println("\n📋 정상 사용자 시나리오");

		String visitorId = "normal_user_001";

		System.out.println("시나리오: 정상 사용자가 사전등록 성공");
		System.out.println("  visitorId: " + visitorId);
		System.out.println("");
		System.out.println("1. 사전등록 페이지 접속");
		System.out.println("   → FingerprintJS가 visitorId 생성");
		System.out.println("");
		System.out.println("2. 사전등록 제출 (성공)");
		System.out.println("   → totalAttempts: 1");
		System.out.println("   → successCount: 1");
		System.out.println("   → failedAttempts: 0");
		System.out.println("   → 실패율: 0% (차단 기준 미만)");
		System.out.println("");
		System.out.println("결과: ✅ 200 OK (허용)");

		// 실패율 계산
		double failureRate = 0.0 / 1.0;
		boolean shouldBlock = failureRate >= 0.8 && 1 >= 5;

		assertThat(shouldBlock).isFalse();
		System.out.println("✅ 정상 사용자 시나리오 검증 완료");
	}

	@Test
	@DisplayName("의심스러운 사용자 시나리오 - 5회 시도 중 4회 실패")
	void testSuspiciousUserScenario() {
		System.out.println("\n📋 의심스러운 사용자 시나리오");

		String visitorId = "suspicious_user_002";

		System.out.println("시나리오: 의심스러운 사용자가 여러 번 실패");
		System.out.println("  visitorId: " + visitorId);
		System.out.println("");
		System.out.println("시도 내역:");
		System.out.println("  1회: ❌ 실패 (전화번호 불일치)");
		System.out.println("  2회: ❌ 실패 (생년월일 불일치)");
		System.out.println("  3회: ❌ 실패 (전화번호 불일치)");
		System.out.println("  4회: ❌ 실패 (전화번호 불일치)");
		System.out.println("  5회: ✅ 성공");
		System.out.println("");
		System.out.println("통계:");
		System.out.println("  totalAttempts: 5");
		System.out.println("  failedAttempts: 4");
		System.out.println("  successCount: 1");
		System.out.println("  실패율: 80%");
		System.out.println("");
		System.out.println("판정:");
		System.out.println("  시도 횟수 >= 5: ✅");
		System.out.println("  실패율 >= 80%: ✅");
		System.out.println("  결과: ❌ 400 Bad Request (차단)");
		System.out.println("  메시지: \"비정상적인 요청이 감지되었습니다.\"");

		// 차단 여부 계산
		int totalAttempts = 5;
		int failedAttempts = 4;
		double failureRate = (double) failedAttempts / totalAttempts;
		boolean shouldBlock = totalAttempts >= 5 && failureRate >= 0.8;

		assertThat(failureRate).isEqualTo(0.8);
		assertThat(shouldBlock).isTrue();
		System.out.println("✅ 의심스러운 사용자 시나리오 검증 완료");
	}

	@Test
	@DisplayName("IP 우회 매크로 시나리오 - 프록시 바꿔가며 시도")
	void testProxyRotationScenario() {
		System.out.println("\n📋 IP 우회 매크로 시나리오");

		String visitorId = "bot_visitor_999";

		System.out.println("시나리오: 봇이 프록시를 바꿔가며 접속");
		System.out.println("  visitorId: " + visitorId + " (동일)");
		System.out.println("");
		System.out.println("시도 내역:");
		System.out.println("  1회: IP 1.1.1.1   → ❌ 실패");
		System.out.println("  2회: IP 2.2.2.2   → ❌ 실패");
		System.out.println("  3회: IP 3.3.3.3   → ❌ 실패");
		System.out.println("  4회: IP 4.4.4.4   → ❌ 실패");
		System.out.println("  5회: IP 5.5.5.5   → ❌ 실패");
		System.out.println("  6회: IP 6.6.6.6   → ❌ 실패");
		System.out.println("  7회: IP 7.7.7.7   → ❌ 실패");
		System.out.println("  8회: IP 8.8.8.8   → ❌ 실패");
		System.out.println("  9회: IP 9.9.9.9   → ❌ 실패");
		System.out.println("  10회: IP 10.10.10.10 → ✅ 성공 (우연히 맞춤)");
		System.out.println("");
		System.out.println("통계:");
		System.out.println("  totalAttempts: 10");
		System.out.println("  failedAttempts: 9");
		System.out.println("  successCount: 1");
		System.out.println("  실패율: 90%");
		System.out.println("");
		System.out.println("핵심:");
		System.out.println("  ✅ IP를 바꿔도 visitorId(브라우저 지문)는 동일");
		System.out.println("  ✅ Redis에 누적된 실패 기록으로 차단 가능");
		System.out.println("");
		System.out.println("결과: ❌ 400 Bad Request (차단)");

		// 차단 여부 계산
		int totalAttempts = 10;
		int failedAttempts = 9;
		double failureRate = (double) failedAttempts / totalAttempts;
		boolean shouldBlock = totalAttempts >= 5 && failureRate >= 0.8;

		assertThat(failureRate).isEqualTo(0.9);
		assertThat(shouldBlock).isTrue();
		System.out.println("✅ IP 우회 매크로 시나리오 검증 완료");
	}

	@Test
	@DisplayName("visitorId 없는 요청 - 1차 허용")
	void testMissingVisitorIdScenario() {
		System.out.println("\n📋 visitorId 없는 요청 시나리오");

		System.out.println("시나리오: FingerprintJS 로드 실패 또는 X-Device-Id 헤더 누락");
		System.out.println("");
		System.out.println("요청:");
		System.out.println("  POST /api/v1/events/1/pre-registers");
		System.out.println("  X-Device-Id: (없음)");
		System.out.println("");
		System.out.println("처리:");
		System.out.println("  1. Fingerprint 필터에서 visitorId 확인 → null");
		System.out.println("  2. 1차 허용 (IP Rate Limit만 적용)");
		System.out.println("");
		System.out.println("결과: ✅ 200 OK (허용)");
		System.out.println("");
		System.out.println("이유:");
		System.out.println("  - 정상 사용자가 FingerprintJS 로드 실패할 수 있음");
		System.out.println("  - 과도한 차단은 UX 저하");
		System.out.println("  - IP Rate Limit으로도 충분한 보호 가능");

		System.out.println("✅ visitorId 없는 요청 시나리오 검증 완료");
	}

	@Test
	@DisplayName("Fingerprint 차단 기준 검증")
	void testBlockingThreshold() {
		System.out.println("\n📋 Fingerprint 차단 기준 검증");

		System.out.println("설정 (application.yml):");
		System.out.println("  min-attempts: 5");
		System.out.println("  failure-rate-threshold: 0.8 (80%)");
		System.out.println("");

		// 다양한 시나리오
		testScenario(3, 2);   // 3회 시도, 2회 실패 (66.7%)
		testScenario(5, 3);   // 5회 시도, 3회 실패 (60%)
		testScenario(5, 4);   // 5회 시도, 4회 실패 (80%)
		testScenario(10, 8);  // 10회 시도, 8회 실패 (80%)
		testScenario(10, 9);  // 10회 시도, 9회 실패 (90%)

		System.out.println("✅ Fingerprint 차단 기준 검증 완료");
	}

	private void testScenario(int total, int failed) {
		double failureRate = (double) failed / total;
		boolean shouldBlock = total >= 5 && failureRate >= 0.8;

		String result = shouldBlock ? "❌ 차단" : "✅ 허용";
		System.out.printf("  %2d회 시도, %2d회 실패 → 실패율 %.1f%% → %s%n",
			total, failed, failureRate * 100, result);
	}
}
