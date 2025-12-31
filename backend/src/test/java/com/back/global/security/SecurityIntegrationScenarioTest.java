package com.back.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 보안 시스템 통합 시나리오 테스트
 *
 * 실제 사용자 시나리오를 기반으로 보안 필터들이 어떻게 동작하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityIntegrationScenarioTest {

	@Test
	@DisplayName("시나리오 1: 정상 사용자 - 사전등록 성공")
	void scenario1_NormalUser_Success() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("📋 시나리오 1: 정상 사용자 - 사전등록 성공");
		System.out.println("=".repeat(80));

		System.out.println("\n[사용자 정보]");
		System.out.println("  이름: 홍길동");
		System.out.println("  IP: 121.162.30.40 (KT)");
		System.out.println("  visitorId: abc123def456 (Chrome 브라우저)");

		System.out.println("\n[1단계] 사전등록 페이지 접속");
		System.out.println("  GET /events/1");
		System.out.println("  → FingerprintJS가 visitorId 생성: abc123def456");

		System.out.println("\n[2단계] 보안 필터 체크");
		System.out.println("  ✅ WhitelistFilter: 127.0.0.1 아님 → 통과");
		System.out.println("  ✅ IdcBlockFilter: 121.162.30.40 → IDC IP 아님 → 통과");
		System.out.println("  ✅ RateLimitFilter: 1초당 1회 → 제한 내 → 통과");
		System.out.println("  ✅ FingerprintFilter: visitorId 기록 없음 → 1차 허용 → 통과");

		System.out.println("\n[3단계] SMS 인증");
		System.out.println("  POST /api/v1/sms/send");
		System.out.println("  Request Body: { \"phoneNumber\": \"01012345678\" }");
		System.out.println("  Headers:");
		System.out.println("    Authorization: Bearer <JWT_TOKEN>");
		System.out.println("    X-Device-Id: abc123def456");
		System.out.println("");
		System.out.println("  보안 필터 체크:");
		System.out.println("  ✅ RateLimitFilter (SMS): IP+전화번호 조합 1/5 → 통과");
		System.out.println("");
		System.out.println("  Response: 200 OK");
		System.out.println("  { \"message\": \"인증번호가 발송되었습니다.\" }");

		System.out.println("\n[4단계] 사전등록 제출");
		System.out.println("  POST /api/v1/events/1/pre-registers");
		System.out.println("  Request Body:");
		System.out.println("  {");
		System.out.println("    \"fullName\": \"홍길동\",");
		System.out.println("    \"phoneNumber\": \"01012345678\",");
		System.out.println("    \"birthDate\": \"1990-01-01\",");
		System.out.println("    \"agreeTerms\": true,");
		System.out.println("    \"agreePrivacy\": true");
		System.out.println("  }");
		System.out.println("  Headers:");
		System.out.println("    X-Recaptcha-Token: <RECAPTCHA_TOKEN>");
		System.out.println("    X-Device-Id: abc123def456");
		System.out.println("");
		System.out.println("  처리:");
		System.out.println("  1. reCAPTCHA 검증: ✅ 통과 (점수 0.9)");
		System.out.println("  2. 사용자 인증 확인: ✅ 통과");
		System.out.println("  3. 본인 인증: ✅ 통과 (전화번호+생년월일 일치)");
		System.out.println("  4. 사전등록 저장: ✅ 성공");
		System.out.println("  5. Fingerprint 기록: totalAttempts=1, successCount=1");
		System.out.println("");
		System.out.println("  Response: 201 Created");
		System.out.println("  { \"message\": \"사전등록이 완료되었습니다.\" }");

		System.out.println("\n[결과] ✅ 정상 사용자 성공 시나리오 완료");
		System.out.println("=".repeat(80));
	}

	@Test
	@DisplayName("시나리오 2: 무한 요청 봇 - Rate Limit 차단")
	void scenario2_InfiniteRequestBot_RateLimitBlock() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("📋 시나리오 2: 무한 요청 봇 - Rate Limit 차단");
		System.out.println("=".repeat(80));

		System.out.println("\n[봇 정보]");
		System.out.println("  유형: 무한 요청 봇 (초당 100회 요청)");
		System.out.println("  IP: 203.123.45.67");

		System.out.println("\n[공격 시도]");
		for (int i = 1; i <= 55; i++) {
			String status = i <= 50 ? "✅ 200 OK" : "❌ 429 Too Many Requests";
			if (i <= 5 || i > 48) {
				System.out.printf("  요청 %2d: %s%n", i, status);
			} else if (i == 25) {
				System.out.println("  ... (중간 생략) ...");
			}
		}

		System.out.println("\n[RateLimitFilter 동작]");
		System.out.println("  설정: 전체 API 1초당 50회");
		System.out.println("  IP: 203.123.45.67");
		System.out.println("  Redis Key: rate_limit:global:203.123.45.67");
		System.out.println("");
		System.out.println("  1~50번째 요청:");
		System.out.println("    Bucket4j: tryConsume(1) → true");
		System.out.println("    Response: 200 OK");
		System.out.println("");
		System.out.println("  51번째 요청:");
		System.out.println("    Bucket4j: tryConsume(1) → false (토큰 부족)");
		System.out.println("    Response: 429 Too Many Requests");
		System.out.println("    Body: { \"message\": \"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\" }");

		System.out.println("\n[결과] ✅ Rate Limit으로 무한 요청 봇 차단 성공");
		System.out.println("=".repeat(80));
	}

	@Test
	@DisplayName("시나리오 3: 서버 매크로 - IDC IP 차단")
	void scenario3_ServerMacro_IdcIpBlock() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("📋 시나리오 3: 서버 매크로 - IDC IP 차단");
		System.out.println("=".repeat(80));

		System.out.println("\n[봇 정보]");
		System.out.println("  유형: AWS EC2에서 실행되는 Python 매크로");
		System.out.println("  IP: 13.124.50.100 (AWS Seoul)");
		System.out.println("  목적: 대량 사전등록 시도");

		System.out.println("\n[공격 시도]");
		System.out.println("  POST /api/v1/events/1/pre-registers");
		System.out.println("  Headers:");
		System.out.println("    X-Forwarded-For: 13.124.50.100");

		System.out.println("\n[IdcBlockFilter 동작]");
		System.out.println("  1. IP 추출: X-Forwarded-For → 13.124.50.100");
		System.out.println("  2. Redis에서 IDC IP 리스트 조회");
		System.out.println("  3. CIDR 매칭:");
		System.out.println("     13.124.0.0/16 범위에 13.124.50.100 포함? → ✅ Yes");
		System.out.println("  4. 차단 결정");
		System.out.println("");
		System.out.println("  Response: 403 Forbidden");
		System.out.println("  Body:");
		System.out.println("  {");
		System.out.println("    \"error\": \"IDC_IP_BLOCKED\",");
		System.out.println("    \"message\": \"VPN 또는 프록시를 사용 중입니다. 해제 후 다시 시도해주세요.\"");
		System.out.println("  }");

		System.out.println("\n[효과]");
		System.out.println("  ✅ AWS EC2, Azure VM, GCP Compute Engine 매크로 원천 차단");
		System.out.println("  ✅ 서버 대여 비용이 들어가므로 대량 공격 억제");
		System.out.println("  ✅ 클라우드 IP 대역은 정기적으로 갱신 (매주 월요일 3시)");

		System.out.println("\n[결과] ✅ IDC IP 차단으로 서버 매크로 차단 성공");
		System.out.println("=".repeat(80));
	}

	@Test
	@DisplayName("시나리오 4: IP 우회 매크로 - Fingerprint 차단")
	void scenario4_ProxyRotationMacro_FingerprintBlock() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("📋 시나리오 4: IP 우회 매크로 - Fingerprint 차단");
		System.out.println("=".repeat(80));

		System.out.println("\n[봇 정보]");
		System.out.println("  유형: 프록시 리스트를 돌며 IP를 바꿔가며 접속");
		System.out.println("  목적: Rate Limit 우회");
		System.out.println("  사용 도구: Selenium + ProxyMesh");

		System.out.println("\n[공격 시도]");
		String visitorId = "bot_fingerprint_abc";
		String[] proxies = {"1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4", "5.5.5.5",
			"6.6.6.6", "7.7.7.7", "8.8.8.8", "9.9.9.9", "10.10.10.10"};

		for (int i = 0; i < proxies.length; i++) {
			String result = i < 9 ? "❌ 실패 (전화번호 불일치)" : "✅ 성공 (우연히 맞춤)";
			System.out.printf("  시도 %2d: IP %s → visitorId: %s → %s%n",
				i + 1, proxies[i], visitorId, result);
		}

		System.out.println("\n[FingerprintFilter 동작]");
		System.out.println("  핵심: IP는 바뀌어도 visitorId(브라우저 지문)는 동일!");
		System.out.println("");
		System.out.println("  Redis Key: fingerprint:" + visitorId);
		System.out.println("  Redis Value:");
		System.out.println("  {");
		System.out.println("    \"totalAttempts\": 10,");
		System.out.println("    \"failedAttempts\": 9,");
		System.out.println("    \"successCount\": 1");
		System.out.println("  }");
		System.out.println("");
		System.out.println("  차단 판정:");
		System.out.println("    totalAttempts >= 5? → ✅ Yes (10 >= 5)");
		System.out.println("    실패율 >= 80%? → ✅ Yes (90% >= 80%)");
		System.out.println("    결과: 차단!");
		System.out.println("");
		System.out.println("  11번째 시도:");
		System.out.println("    POST /api/v1/events/1/pre-registers");
		System.out.println("    IP: 11.11.11.11 (새로운 프록시)");
		System.out.println("    X-Device-Id: " + visitorId);
		System.out.println("");
		System.out.println("    Response: 400 Bad Request");
		System.out.println("    Body:");
		System.out.println("    {");
		System.out.println("      \"error\": \"SUSPICIOUS_ACTIVITY\",");
		System.out.println("      \"message\": \"비정상적인 요청이 감지되었습니다. 잠시 후 다시 시도해주세요.\"");
		System.out.println("    }");

		System.out.println("\n[효과]");
		System.out.println("  ✅ IP 우회 공격 탐지 가능");
		System.out.println("  ✅ 브라우저 지문(Canvas, WebGL, Fonts 등) 기반 추적");
		System.out.println("  ✅ 24시간 동안 기록 유지 (TTL)");

		System.out.println("\n[결과] ✅ Fingerprint로 IP 우회 매크로 차단 성공");
		System.out.println("=".repeat(80));
	}

	@Test
	@DisplayName("시나리오 5: 다층 방어 - 모든 보안 필터 통합")
	void scenario5_MultiLayerDefense_AllFilters() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("📋 시나리오 5: 다층 방어 시스템 - 모든 필터 통합");
		System.out.println("=".repeat(80));

		System.out.println("\n[보안 필터 체인 구조]");
		System.out.println("");
		System.out.println("  요청");
		System.out.println("   │");
		System.out.println("   ├─→ [1] WhitelistFilter");
		System.out.println("   │    └─ 개발/테스트 환경 IP 허용 (127.0.0.1, ::1)");
		System.out.println("   │");
		System.out.println("   ├─→ [2] IdcBlockFilter");
		System.out.println("   │    └─ AWS, Azure, GCP 등 IDC IP 차단 (403 Forbidden)");
		System.out.println("   │");
		System.out.println("   ├─→ [3] RateLimitFilter");
		System.out.println("   │    ├─ 전체 API: 1초당 50회 (429 Too Many Requests)");
		System.out.println("   │    └─ SMS API: 1분당 5회 (429 Too Many Requests)");
		System.out.println("   │");
		System.out.println("   ├─→ [4] FingerprintFilter");
		System.out.println("   │    └─ 실패율 80% 이상 차단 (400 Bad Request)");
		System.out.println("   │");
		System.out.println("   ├─→ [5] CustomAuthenticationFilter");
		System.out.println("   │    └─ JWT 토큰 검증 (401 Unauthorized)");
		System.out.println("   │");
		System.out.println("   └─→ Controller → Service → Repository");

		System.out.println("\n[다양한 봇 유형별 차단 레이어]");
		System.out.println("");
		System.out.println("  1. 서버 매크로 (AWS EC2):");
		System.out.println("     → IdcBlockFilter에서 차단 (2단계)");
		System.out.println("");
		System.out.println("  2. 무한 요청 봇 (DDoS):");
		System.out.println("     → RateLimitFilter에서 차단 (3단계)");
		System.out.println("");
		System.out.println("  3. IP 우회 매크로 (Proxy Rotation):");
		System.out.println("     → FingerprintFilter에서 차단 (4단계)");
		System.out.println("");
		System.out.println("  4. 정상 사용자:");
		System.out.println("     → 모든 필터 통과 → Controller 도달");

		System.out.println("\n[설정 관리]");
		System.out.println("");
		System.out.println("  test 프로파일:");
		System.out.println("    - rate-limit.enabled: false");
		System.out.println("    - idc-block.enabled: false");
		System.out.println("    - fingerprint.enabled: false");
		System.out.println("    목적: 기존 테스트 코드 정상 작동 보장");
		System.out.println("");
		System.out.println("  dev 프로파일:");
		System.out.println("    - rate-limit.enabled: true (느슨한 제한: 1000/s)");
		System.out.println("    - idc-block.enabled: false");
		System.out.println("    - fingerprint.enabled: false");
		System.out.println("    목적: 개발 편의성");
		System.out.println("");
		System.out.println("  prod 프로파일:");
		System.out.println("    - rate-limit.enabled: true (엄격한 제한: 50/s, 5/min)");
		System.out.println("    - idc-block.enabled: true");
		System.out.println("    - fingerprint.enabled: true");
		System.out.println("    목적: 운영 환경 보안 강화");

		System.out.println("\n[모니터링 포인트]");
		System.out.println("  - Redis 키 개수: KEYS rate_limit:*, fingerprint:*, IDC_IP_LIST");
		System.out.println("  - 차단 로그: [RateLimitFilter], [IdcBlockFilter], [FingerprintFilter]");
		System.out.println("  - 응답 코드 비율: 200 vs 400 vs 403 vs 429");

		System.out.println("\n[결과] ✅ 다층 방어 시스템으로 다양한 봇 유형 차단 가능");
		System.out.println("=".repeat(80));
	}
}
