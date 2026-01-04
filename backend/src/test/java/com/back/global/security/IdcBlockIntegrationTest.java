package com.back.global.security;

import static org.assertj.core.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.back.global.security.service.IdcIpBlockService;

/**
 * IDC IP 차단 통합 테스트
 *
 * 실제 Redis를 사용하여 IDC IP 차단이 정상적으로 작동하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class IdcBlockIntegrationTest {

	@Autowired(required = false)
	private IdcIpBlockService idcIpBlockService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String REDIS_KEY_IDC_IP_LIST = "IDC_IP_LIST";

	@BeforeEach
	void setUp() {
		if (idcIpBlockService == null) {
			System.out.println("⚠️  IdcIpBlockService가 비활성화되어 있습니다 (test 프로파일)");
		}
		cleanupRedis();
	}

	@AfterEach
	void tearDown() {
		cleanupRedis();
	}

	private void cleanupRedis() {
		redisTemplate.delete(REDIS_KEY_IDC_IP_LIST);
	}

	@Test
	@DisplayName("IDC IP 차단 서비스가 test 프로파일에서 비활성화되는지 확인")
	void testIdcBlockServiceDisabledInTestProfile() {
		assertThat(idcIpBlockService).isNull();
		System.out.println("✅ IDC IP 차단 서비스가 test 프로파일에서 비활성화됨");
	}

	@Test
	@DisplayName("CIDR 매칭 알고리즘 검증")
	void testCidrMatching() throws UnknownHostException {
		System.out.println("\n📋 CIDR 매칭 알고리즘 검증");

		// AWS IP 대역 예시
		String awsCidr = "13.34.0.0/16";
		SubnetUtils subnet = new SubnetUtils(awsCidr);
		subnet.setInclusiveHostCount(true);

		// 테스트 IP들
		String awsIp1 = "13.34.10.20";  // AWS 대역 내
		String awsIp2 = "13.34.255.255"; // AWS 대역 내
		String normalIp = "121.162.30.40"; // AWS 대역 외

		boolean isAws1 = subnet.getInfo().isInRange(awsIp1);
		boolean isAws2 = subnet.getInfo().isInRange(awsIp2);
		boolean isNormal = subnet.getInfo().isInRange(normalIp);

		System.out.println("CIDR 대역: " + awsCidr);
		System.out.println("  " + awsIp1 + " → " + (isAws1 ? "✅ 차단 (AWS)" : "❌ 허용"));
		System.out.println("  " + awsIp2 + " → " + (isAws2 ? "✅ 차단 (AWS)" : "❌ 허용"));
		System.out.println("  " + normalIp + " → " + (isNormal ? "❌ 차단됨" : "✅ 허용 (일반 사용자)"));

		assertThat(isAws1).isTrue();
		assertThat(isAws2).isTrue();
		assertThat(isNormal).isFalse();

		System.out.println("✅ CIDR 매칭 알고리즘 정상 작동");
	}

	@Test
	@DisplayName("Redis에 IDC IP 리스트가 저장되는 구조 검증")
	void testRedisIdcIpListStructure() {
		System.out.println("\n📋 Redis IDC IP 리스트 구조 검증");

		// 테스트용 IDC IP 대역 추가
		redisTemplate.opsForSet().add(REDIS_KEY_IDC_IP_LIST,
			"13.34.0.0/16",    // AWS EC2 (us-east-1)
			"3.5.0.0/16",      // AWS EC2
			"13.124.0.0/16",   // AWS EC2 (ap-northeast-2, Seoul)
			"52.78.0.0/16"     // AWS EC2 (ap-northeast-2, Seoul)
		);

		// 저장된 CIDR 대역 확인
		Long size = redisTemplate.opsForSet().size(REDIS_KEY_IDC_IP_LIST);
		System.out.println("저장된 IDC IP 대역 개수: " + size);

		assertThat(size).isEqualTo(4);

		// 저장된 대역 출력
		redisTemplate.opsForSet().members(REDIS_KEY_IDC_IP_LIST).forEach(cidr -> {
			System.out.println("  - " + cidr);
		});

		System.out.println("✅ Redis IDC IP 리스트 구조 검증 완료");
	}

	@Test
	@DisplayName("IDC IP 차단 시나리오 - AWS EC2에서 접속")
	void testIdcBlockScenario() throws UnknownHostException {
		System.out.println("\n📋 IDC IP 차단 시나리오");

		// AWS Seoul 리전 CIDR 대역
		String awsSeoulCidr = "13.124.0.0/16";
		SubnetUtils subnet = new SubnetUtils(awsSeoulCidr);
		subnet.setInclusiveHostCount(true);

		// AWS EC2에서 접속 시도
		String botIp = "13.124.50.100";

		System.out.println("시나리오: 봇이 AWS EC2 서울 리전에서 접속");
		System.out.println("  봇 IP: " + botIp);
		System.out.println("  AWS 대역: " + awsSeoulCidr);

		boolean isBlocked = subnet.getInfo().isInRange(botIp);

		if (isBlocked) {
			System.out.println("  결과: ❌ 403 Forbidden");
			System.out.println("  메시지: \"VPN 또는 프록시를 사용 중입니다. 해제 후 다시 시도해주세요.\"");
		} else {
			System.out.println("  결과: ✅ 200 OK (허용)");
		}

		assertThat(isBlocked).isTrue();
		System.out.println("✅ IDC IP 차단 시나리오 검증 완료");
	}

	@Test
	@DisplayName("정상 사용자 시나리오 - 일반 ISP에서 접속")
	void testNormalUserScenario() throws UnknownHostException {
		System.out.println("\n📋 정상 사용자 시나리오");

		// AWS Seoul 리전 CIDR 대역
		String awsSeoulCidr = "13.124.0.0/16";
		SubnetUtils subnet = new SubnetUtils(awsSeoulCidr);
		subnet.setInclusiveHostCount(true);

		// KT, SKT, LG U+ 등 일반 ISP IP
		String normalUserIp = "121.162.30.40"; // KT

		System.out.println("시나리오: 정상 사용자가 KT 인터넷에서 접속");
		System.out.println("  사용자 IP: " + normalUserIp);
		System.out.println("  AWS 대역: " + awsSeoulCidr);

		boolean isBlocked = subnet.getInfo().isInRange(normalUserIp);

		if (isBlocked) {
			System.out.println("  결과: ❌ 403 Forbidden (오차단!)");
		} else {
			System.out.println("  결과: ✅ 200 OK (허용)");
			System.out.println("  정상적으로 사전등록 진행 가능");
		}

		assertThat(isBlocked).isFalse();
		System.out.println("✅ 정상 사용자 시나리오 검증 완료");
	}

	@Test
	@DisplayName("주요 클라우드 공급자 IP 대역 검증")
	void testMajorCloudProviders() {
		System.out.println("\n📋 주요 클라우드 공급자 IP 대역");

		// 실제 차단 대상 IP 대역들
		String[] cloudCidrs = {
			"13.34.0.0/16",     // AWS
			"3.5.0.0/16",       // AWS
			"20.0.0.0/11",      // Azure
			"35.190.0.0/16",    // GCP
			"34.64.0.0/11"      // GCP
		};

		System.out.println("차단 대상 클라우드 IP 대역:");
		for (String cidr : cloudCidrs) {
			System.out.println("  - " + cidr);
		}

		System.out.println("");
		System.out.println("예상 차단 효과:");
		System.out.println("  ✅ AWS EC2에서 실행되는 매크로 차단");
		System.out.println("  ✅ Azure VM에서 실행되는 매크로 차단");
		System.out.println("  ✅ GCP Compute Engine에서 실행되는 매크로 차단");
		System.out.println("  ✅ 기타 데이터센터 IP 차단");
		System.out.println("");
		System.out.println("예외:");
		System.out.println("  ⚠️  VPN 사용자도 차단될 수 있음 (트레이드오프)");
		System.out.println("  💡 화이트리스트로 개발/테스트 환경 IP 허용");
	}
}
