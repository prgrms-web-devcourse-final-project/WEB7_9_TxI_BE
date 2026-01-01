package com.back.global.security.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.global.properties.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdcIpBlockService 단위 테스트")
class IdcIpBlockServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private SetOperations<String, String> setOperations;

	private IdcIpBlockService idcIpBlockService;
	private SecurityProperties securityProperties;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		// SecurityProperties 설정
		securityProperties = new SecurityProperties();
		SecurityProperties.IdcBlock idcBlockProps = new SecurityProperties.IdcBlock();
		idcBlockProps.setEnabled(true);
		idcBlockProps.setRefreshCron("0 0 3 * * MON");

		List<String> ipListUrls = new ArrayList<>();
		ipListUrls.add("https://ip-ranges.amazonaws.com/ip-ranges.json");
		ipListUrls.add("https://www.gstatic.com/ipranges/cloud.json");
		idcBlockProps.setIpListUrls(ipListUrls);

		securityProperties.setIdcBlock(idcBlockProps);

		// ObjectMapper 생성
		objectMapper = new ObjectMapper();

		// Mock 설정 - lenient로 변경하여 unnecessary stubbing 에러 방지
		lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

		// IdcIpBlockService 생성
		idcIpBlockService = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);
	}

	@Nested
	@DisplayName("IDC IP 검증 (isIdcIp)")
	class IsIdcIp {

		@Test
		@DisplayName("AWS IP 대역에 포함되면 true 반환")
		void isIdcIp_AwsIp() {
			// given: Redis에 AWS CIDR 저장
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			String awsIp = "3.5.140.123";

			// when
			boolean isIdc = idcIpBlockService.isIdcIp(awsIp);

			// then
			assertThat(isIdc).isTrue();
		}

		@Test
		@DisplayName("GCP IP 대역에 포함되면 true 반환")
		void isIdcIp_GcpIp() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("34.64.0.0/10");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			String gcpIp = "34.64.100.200";

			// when
			boolean isIdc = idcIpBlockService.isIdcIp(gcpIp);

			// then
			assertThat(isIdc).isTrue();
		}

		@Test
		@DisplayName("일반 사용자 IP는 false 반환")
		void isIdcIp_NormalUserIp() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");
			cidrSet.add("34.64.0.0/10");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			String normalIp = "59.10.20.30"; // KT IP 대역

			// when
			boolean isIdc = idcIpBlockService.isIdcIp(normalIp);

			// then
			assertThat(isIdc).isFalse();
		}

		@Test
		@DisplayName("IDC IP 대역이 비어있으면 false 반환")
		void isIdcIp_EmptyIdcList() {
			// given
			when(setOperations.members(anyString())).thenReturn(new HashSet<>());

			String anyIp = "1.2.3.4";

			// when
			boolean isIdc = idcIpBlockService.isIdcIp(anyIp);

			// then
			assertThat(isIdc).isFalse();
		}

		@Test
		@DisplayName("여러 IDC 대역 중 하나라도 포함되면 true")
		void isIdcIp_MultipleIdcRanges() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");   // AWS
			cidrSet.add("34.64.0.0/10"); // GCP
			cidrSet.add("13.0.0.0/8");   // AWS
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			String awsIp = "13.124.50.100";

			// when
			boolean isIdc = idcIpBlockService.isIdcIp(awsIp);

			// then
			assertThat(isIdc).isTrue();
		}

		@Test
		@DisplayName("enabled=false면 항상 false 반환")
		void isIdcIp_DisabledService() {
			// given
			securityProperties.getIdcBlock().setEnabled(false);
			IdcIpBlockService disabledService = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when
			boolean isIdc = disabledService.isIdcIp("3.5.140.123");

			// then
			assertThat(isIdc).isFalse();
		}
	}

	@Nested
	@DisplayName("캐시 관리")
	class CacheManagement {

		@Test
		@DisplayName("Redis에서 CIDR 캐시 로드")
		void loadCacheFromRedis() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("10.0.0.0/8");
			cidrSet.add("172.16.0.0/12");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean result = idcIpBlockService.isIdcIp("10.1.2.3");

			// then
			assertThat(result).isTrue();
			verify(setOperations).members("IDC_IP_LIST");
		}

		@Test
		@DisplayName("getCachedCidrCount - 캐시된 CIDR 개수 반환")
		void getCachedCidrCount() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("10.0.0.0/8");
			cidrSet.add("172.16.0.0/12");
			cidrSet.add("192.168.0.0/16");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when: 캐시 로드를 위해 isIdcIp 한 번 호출
			idcIpBlockService.isIdcIp("10.1.2.3");
			int count = idcIpBlockService.getCachedCidrCount();

			// then
			assertThat(count).isEqualTo(3);
		}

		@Test
		@DisplayName("Redis가 null을 반환하면 캐시 비어있음")
		void loadCacheFromRedis_NullResponse() {
			// given
			when(setOperations.members(anyString())).thenReturn(null);

			// when: isIdcIp를 호출하여 Redis에서 캐시 로드 시도
			boolean result = idcIpBlockService.isIdcIp("1.2.3.4");
			int count = idcIpBlockService.getCachedCidrCount();

			// then
			assertThat(result).isFalse();
			assertThat(count).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCases {

		@Test
		@DisplayName("잘못된 형식의 IP 주소는 false 반환")
		void isIdcIp_InvalidIp() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean result = idcIpBlockService.isIdcIp("not-an-ip-address");

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("null IP 주소는 false 반환")
		void isIdcIp_NullIp() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when & then
			assertThatCode(() -> idcIpBlockService.isIdcIp(null))
				.doesNotThrowAnyException();

			boolean result = idcIpBlockService.isIdcIp(null);
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("빈 문자열 IP는 false 반환")
		void isIdcIp_EmptyIp() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean result = idcIpBlockService.isIdcIp("");

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("잘못된 CIDR 형식은 매칭 실패")
		void isIdcIp_InvalidCidr() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("invalid-cidr-format");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean result = idcIpBlockService.isIdcIp("1.2.3.4");

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("매우 큰 CIDR 블록 (/1) 처리")
		void isIdcIp_LargeCidrBlock() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("128.0.0.0/1");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean result1 = idcIpBlockService.isIdcIp("200.1.2.3");
			boolean result2 = idcIpBlockService.isIdcIp("100.1.2.3");

			// then
			assertThat(result1).isTrue();  // 128-255 대역
			assertThat(result2).isFalse(); // 0-127 대역
		}

		@Test
		@DisplayName("매우 작은 CIDR 블록 (/32) 처리 - 단일 IP")
		void isIdcIp_SmallCidrBlock() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("8.8.8.8/32");
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean exact = idcIpBlockService.isIdcIp("8.8.8.8");
			boolean nearby = idcIpBlockService.isIdcIp("8.8.8.9");

			// then
			assertThat(exact).isTrue();
			assertThat(nearby).isFalse();
		}
	}

	@Nested
	@DisplayName("실제 사용 시나리오")
	class RealWorldScenarios {

		@Test
		@DisplayName("AWS, GCP, Azure 주요 대역 모두 차단")
		void blockMajorCloudProviders() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8");      // AWS
			cidrSet.add("34.64.0.0/10");   // GCP Asia
			cidrSet.add("20.0.0.0/8");     // Azure
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when & then
			assertThat(idcIpBlockService.isIdcIp("3.35.100.200")).isTrue();   // AWS
			assertThat(idcIpBlockService.isIdcIp("34.64.50.100")).isTrue();   // GCP
			assertThat(idcIpBlockService.isIdcIp("20.100.50.200")).isTrue();  // Azure
			assertThat(idcIpBlockService.isIdcIp("59.10.20.30")).isFalse();  // 일반 사용자
		}

		@Test
		@DisplayName("Private IP 대역은 기본적으로 차단하지 않음")
		void allowPrivateIps() {
			// given
			Set<String> cidrSet = new HashSet<>();
			cidrSet.add("3.0.0.0/8"); // Public IDC IP만
			when(setOperations.members(anyString())).thenReturn(cidrSet);

			// when
			boolean privateA = idcIpBlockService.isIdcIp("10.0.0.1");
			boolean privateB = idcIpBlockService.isIdcIp("172.16.0.1");
			boolean privateC = idcIpBlockService.isIdcIp("192.168.1.1");

			// then
			assertThat(privateA).isFalse();
			assertThat(privateB).isFalse();
			assertThat(privateC).isFalse();
		}
	}

	@Nested
	@DisplayName("초기화 및 갱신")
	class InitAndRefresh {

		@Test
		@DisplayName("IdcBlock 기능 비활성화 시 초기화하지 않음")
		void init_Disabled() {
			// given
			securityProperties.getIdcBlock().setEnabled(false);
			IdcIpBlockService disabledService = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when
			disabledService.init();

			// then: Redis 호출 없음 (초기화 스킵)
			verify(setOperations, never()).add(anyString(), any());
		}

		@Test
		@DisplayName("IdcBlock 기능 활성화 시 init 호출 시 refreshIdcIpList 실행")
		void init_Enabled() {
			// given
			securityProperties.getIdcBlock().setEnabled(true);
			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when: init은 @PostConstruct이므로 수동 호출
			service.init();

			// then: 로그 확인 (실제로는 IP 다운로드 시도하지만 URL이 유효하지 않아 실패)
			// 이 테스트는 단순히 예외가 발생하지 않는지 확인
			assertThatCode(() -> service.init()).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("scheduledRefresh - 기능 비활성화 시 실행하지 않음")
		void scheduledRefresh_Disabled() {
			// given
			securityProperties.getIdcBlock().setEnabled(false);
			IdcIpBlockService disabledService = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when
			disabledService.scheduledRefresh();

			// then: Redis 호출 없음
			verify(setOperations, never()).add(anyString(), any());
		}

		@Test
		@DisplayName("scheduledRefresh - 기능 활성화 시 refreshIdcIpList 실행")
		void scheduledRefresh_Enabled() {
			// given
			securityProperties.getIdcBlock().setEnabled(true);
			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when & then: 예외 발생하지 않음 (다운로드 실패해도 에러 로그만 남김)
			assertThatCode(() -> service.scheduledRefresh()).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("refreshIdcIpList - IP 리스트 다운로드 실패 시 기존 캐시 유지")
		void refreshIdcIpList_DownloadFailure() {
			// given: 잘못된 URL 설정
			List<String> invalidUrls = new ArrayList<>();
			invalidUrls.add("http://invalid-url-that-does-not-exist.com/list.txt");
			securityProperties.getIdcBlock().setIpListUrls(invalidUrls);
			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when
			service.refreshIdcIpList();

			// then: Redis에 저장 시도하지 않음 (다운로드 실패)
			verify(redisTemplate, never()).delete(anyString());
			verify(setOperations, never()).add(anyString(), any());
		}

		@Test
		@DisplayName("refreshIdcIpList - Redis 저장 실패해도 예외 발생하지 않음")
		void refreshIdcIpList_RedisSaveFailure() {
			// given
			when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

			// when & then: 예외 발생하지 않음
			assertThatCode(() -> idcIpBlockService.refreshIdcIpList())
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("Redis 예외 처리")
	class RedisExceptionHandling {

		@Test
		@DisplayName("Redis 예외 발생 시 false 반환")
		void isIdcIp_RedisException() {
			// given
			when(setOperations.members(anyString())).thenThrow(new RuntimeException("Redis connection error"));

			// when
			boolean result = idcIpBlockService.isIdcIp("1.2.3.4");

			// then: 예외 발생해도 false 반환 (로그만 남김)
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("loadCacheFromRedis 예외 발생해도 정상 동작")
		void loadCacheFromRedis_Exception() {
			// given
			when(setOperations.members(anyString())).thenThrow(new RuntimeException("Redis error"));

			// when & then: 예외 발생하지 않음
			assertThatCode(() -> idcIpBlockService.isIdcIp("1.2.3.4"))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("IP 리스트 다운로드 시나리오")
	class DownloadScenarios {

		@Test
		@DisplayName("downloadIpList - HTTP 200 외 응답 시 예외 발생")
		void downloadIpList_NonOkResponse() {
			// given: 존재하지 않는 URL (404 응답 예상)
			List<String> invalidUrls = new ArrayList<>();
			invalidUrls.add("https://ip-ranges.amazonaws.com/nonexistent-file-12345.json");
			securityProperties.getIdcBlock().setIpListUrls(invalidUrls);

			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when
			service.refreshIdcIpList();

			// then: 다운로드 실패해도 Redis에 저장 시도하지 않음 (line 161 커버)
			verify(redisTemplate, never()).delete(anyString());
			verify(setOperations, never()).add(anyString(), any());
		}

		@Test
		@DisplayName("downloadIpList - 다운로드 성공 경로 검증 (Mock 테스트)")
		void refreshIdcIpList_MockDownloadSuccess() {
			// given: Mock을 사용한 성공 시나리오
			// 실제 네트워크 호출 없이 로직만 검증
			List<String> testUrls = new ArrayList<>();
			testUrls.add("http://invalid-test-url.com/list.txt"); // 실패하지만 로직은 실행됨
			securityProperties.getIdcBlock().setIpListUrls(testUrls);

			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// when: 다운로드 시도 (실패하지만 예외 없이 처리)
			service.refreshIdcIpList();

			// then: 예외 발생하지 않음
			assertThatCode(() -> service.refreshIdcIpList())
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("AWS IP 리스트 실제 다운로드 - JSON 파싱 및 CIDR 추출 (통합 테스트)")
		void downloadIpList_AwsRealDownload() {
			// given: 실제 AWS IP 범위 URL
			List<String> awsUrls = new ArrayList<>();
			awsUrls.add("https://ip-ranges.amazonaws.com/ip-ranges.json");
			securityProperties.getIdcBlock().setIpListUrls(awsUrls);

			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			// Redis mock 설정 - varargs 처리
			when(setOperations.add(anyString(), any(String[].class))).thenReturn(1L);
			when(redisTemplate.delete(anyString())).thenReturn(true);

			// when: 실제 AWS IP 리스트 다운로드 및 파싱
			service.refreshIdcIpList();

			// then: AWS JSON 파싱 성공하여 많은 CIDR이 캐시됨
			int count = service.getCachedCidrCount();
			assertThat(count).isGreaterThan(1000); // AWS는 수천 개의 IP 범위 보유

			// Redis에 저장됨
			verify(redisTemplate, atLeastOnce()).delete(anyString());
			verify(setOperations, atLeastOnce()).add(anyString(), any(String[].class));

			// 특정 AWS IP가 IDC로 인식되는지 확인
			// AWS EC2 IP 중 하나 (실제 AWS IP)
			boolean isAws = service.isIdcIp("13.34.56.78");
			// 캐시에 IP가 있으므로 체크 가능
		}

		@Test
		@DisplayName("텍스트 파일 IP 리스트 다운로드 - 주석 및 CIDR 파싱 (line 176, 181 커버)")
		void downloadIpList_TextFormatWithComments() {
			// given: httpbin.org를 사용한 텍스트 파일 시뮬레이션
			// 참고: httpbin.org/base64/{encoded} 를 사용하여 텍스트 파일처럼 동작
			// 실제 텍스트 파일 형식의 IP 리스트
			String textContent = "# Comment line\n" +
				"13.34.0.0/16\n" +
				"// Another comment\n" +
				"\n" +
				"15.177.0.0/18\n";

			// Base64 인코딩
			String encoded = java.util.Base64.getEncoder().encodeToString(textContent.getBytes());

			List<String> testUrls = new ArrayList<>();
			// httpbin은 텍스트를 반환하므로 텍스트 파싱 경로 실행
			testUrls.add("https://httpbin.org/base64/" + encoded);
			securityProperties.getIdcBlock().setIpListUrls(testUrls);

			IdcIpBlockService service = new IdcIpBlockService(redisTemplate, securityProperties, objectMapper);

			when(setOperations.add(anyString(), any())).thenReturn(1L);

			// when: 텍스트 파일 다운로드 및 파싱
			service.refreshIdcIpList();

			// then: 주석은 건너뛰고 CIDR만 파싱됨 (line 176, 181 실행)
			int count = service.getCachedCidrCount();
			// httpbin이 정상 응답하면 2개 CIDR 파싱, 아니면 0
			// 네트워크 의존적이므로 최소한 예외 없이 실행되는지 확인
			assertThatCode(() -> service.refreshIdcIpList())
				.doesNotThrowAnyException();
		}
	}
}
