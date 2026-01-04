package com.back.global.security.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.back.global.properties.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IDC(데이터센터) IP 차단 서비스
 *
 * 목적:
 * - AWS, Azure, GCP 등 클라우드 데이터센터 IP 대역에서 오는 요청 차단
 * - 서버 매크로(봇) 원천 봉쇄
 *
 * 동작 방식:
 * 1. 애플리케이션 시작 시 IDC IP 리스트 다운로드
 * 2. CIDR 대역을 Redis Set에 저장
 * 3. 요청이 들어오면 IP가 IDC 대역에 포함되는지 확인
 * 4. 주기적으로 IP 리스트 갱신 (기본: 매주 월요일 새벽 3시)
 *
 * Redis 저장 구조:
 * - Key: "IDC_IP_LIST"
 * - Type: Set
 * - Value: CIDR 대역 문자열 (예: "13.34.0.0/16")
 *
 * 주의사항:
 * - IP 리스트 다운로드 실패 시 기존 캐시 유지 (가용성 우선)
 * - CIDR 매칭은 성능상 이유로 메모리 캐시 + Redis 병행
 * - VPN 사용자도 차단될 수 있음 (에러 메시지로 안내)
 */
@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.bot-protection.idc-block.enabled", havingValue = "true", matchIfMissing = false)
public class IdcIpBlockService {

	private static final String REDIS_KEY_IDC_IP_LIST = "IDC_IP_LIST";
	private static final int CONNECT_TIMEOUT = 10000; // 10초
	private static final int READ_TIMEOUT = 10000; // 10초

	private final StringRedisTemplate redisTemplate;
	private final SecurityProperties securityProperties;
	private final ObjectMapper objectMapper;

	// 메모리 캐시: 성능 최적화를 위해 CIDR 대역을 메모리에 캐싱
	private final Set<String> cidrCache = new HashSet<>();

	/**
	 * 애플리케이션 시작 시 IDC IP 리스트 로드
	 */
	@PostConstruct
	public void init() {
		if (!securityProperties.getIdcBlock().isEnabled()) {
			log.info("[IdcIpBlockService] IDC IP 차단 기능 비활성화됨");
			return;
		}

		log.info("[IdcIpBlockService] 초기화 시작 - IDC IP 리스트 로드 중...");
		refreshIdcIpList();
	}

	/**
	 * 주기적으로 IDC IP 리스트 갱신
	 * Cron: application.yml에서 설정 가능 (기본: 매주 월요일 새벽 3시)
	 */
	@Scheduled(cron = "${security.bot-protection.idc-block.refresh-cron:0 0 3 * * MON}")
	public void scheduledRefresh() {
		if (!securityProperties.getIdcBlock().isEnabled()) {
			return;
		}

		log.info("[IdcIpBlockService] 스케줄러 실행 - IDC IP 리스트 갱신 시작");
		refreshIdcIpList();
	}

	/**
	 * IDC IP 리스트를 다운로드하여 Redis와 메모리 캐시에 저장
	 */
	public void refreshIdcIpList() {
		long startTime = System.currentTimeMillis();

		Set<String> newCidrSet = new HashSet<>();

		// 설정된 URL들에서 IP 리스트 다운로드
		for (String ipListUrl : securityProperties.getIdcBlock().getIpListUrls()) {
			try {
				log.info("[IdcIpBlockService] IP 리스트 다운로드 시작 - URL: {}", ipListUrl);
				Set<String> cidrs = downloadIpList(ipListUrl);
				newCidrSet.addAll(cidrs);
				log.info("[IdcIpBlockService] IP 리스트 다운로드 완료 - URL: {}, 항목 수: {}", ipListUrl, cidrs.size());
			} catch (Exception e) {
				log.error("[IdcIpBlockService] IP 리스트 다운로드 실패 - URL: {}, 오류: {}", ipListUrl, e.getMessage());
				// 다운로드 실패해도 다른 소스는 계속 시도
			}
		}

		if (newCidrSet.isEmpty()) {
			log.warn("[IdcIpBlockService] 다운로드된 IP 리스트가 없음 - 기존 캐시 유지");
			return;
		}

		// Redis에 저장 (기존 데이터 삭제 후 새로 저장)
		try {
			redisTemplate.delete(REDIS_KEY_IDC_IP_LIST);
			redisTemplate.opsForSet().add(REDIS_KEY_IDC_IP_LIST, newCidrSet.toArray(new String[0]));
			// TTL 설정 (1주일)
			redisTemplate.expire(REDIS_KEY_IDC_IP_LIST, 7, TimeUnit.DAYS);
			log.info("[IdcIpBlockService] Redis 저장 완료 - 총 {} 개 CIDR 대역", newCidrSet.size());
		} catch (Exception e) {
			log.error("[IdcIpBlockService] Redis 저장 실패 - 오류: {}", e.getMessage());
		}

		// 메모리 캐시 갱신
		synchronized (cidrCache) {
			cidrCache.clear();
			cidrCache.addAll(newCidrSet);
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("[IdcIpBlockService] IDC IP 리스트 갱신 완료 - 소요 시간: {}ms, 총 {} 개 대역", elapsedTime, newCidrSet.size());
	}

	/**
	 * URL에서 IP 리스트 다운로드
	 *
	 * @param urlString IP 리스트 URL
	 * @return CIDR 대역 Set
	 */
	private Set<String> downloadIpList(String urlString) throws Exception {
		Set<String> cidrSet = new HashSet<>();

		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestMethod("GET");

		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("HTTP 응답 코드: " + responseCode);
		}

		// AWS JSON 형식인지 확인 (ip-ranges.amazonaws.com)
		if (urlString.contains("ip-ranges.amazonaws.com")) {
			cidrSet.addAll(parseAwsIpRangesJson(conn));
		} else {
			// 기존 텍스트 파일 형식 파싱
			try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();

					// 주석이나 빈 줄 무시
					if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
						continue;
					}

					// CIDR 형식 검증 (간단한 체크)
					if (line.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}$")) {
						cidrSet.add(line);
					}
				}
			}
		}

		return cidrSet;
	}

	/**
	 * AWS ip-ranges.json 형식 파싱
	 *
	 * AWS 공식 IP 범위 JSON 형식:
	 * {
	 *   "prefixes": [
	 *     {"ip_prefix": "3.5.140.0/22", "region": "ap-northeast-2", "service": "AMAZON"},
	 *     ...
	 *   ],
	 *   "ipv6_prefixes": [...]
	 * }
	 *
	 * @param conn HTTP 연결
	 * @return CIDR 대역 Set
	 */
	private Set<String> parseAwsIpRangesJson(HttpURLConnection conn) throws Exception {
		Set<String> cidrSet = new HashSet<>();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			StringBuilder jsonBuilder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonBuilder.append(line);
			}

			JsonNode root = objectMapper.readTree(jsonBuilder.toString());

			// IPv4 prefixes 추출
			JsonNode prefixes = root.get("prefixes");
			if (prefixes != null && prefixes.isArray()) {
				for (JsonNode prefix : prefixes) {
					String ipPrefix = prefix.get("ip_prefix").asText();
					if (ipPrefix != null && !ipPrefix.isEmpty()) {
						cidrSet.add(ipPrefix);
					}
				}
			}

			log.info("[IdcIpBlockService] AWS IP 범위 JSON 파싱 완료 - IPv4 개수: {}", cidrSet.size());
		}

		return cidrSet;
	}

	/**
	 * IP가 IDC 대역에 포함되는지 확인
	 *
	 * @param ip 클라이언트 IP
	 * @return IDC IP 여부
	 */
	public boolean isIdcIp(String ip) {
		if (!securityProperties.getIdcBlock().isEnabled()) {
			return false;
		}

		// 메모리 캐시가 비어있으면 Redis에서 로드
		if (cidrCache.isEmpty()) {
			loadCacheFromRedis();
		}

		// 메모리 캐시에서 CIDR 매칭
		synchronized (cidrCache) {
			for (String cidr : cidrCache) {
				if (isIpInCidr(ip, cidr)) {
					log.info("[IdcIpBlockService] IDC IP 감지 - IP: {}, CIDR: {}", ip, cidr);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Redis에서 CIDR 캐시 로드
	 */
	private void loadCacheFromRedis() {
		try {
			Set<String> cidrsFromRedis = redisTemplate.opsForSet().members(REDIS_KEY_IDC_IP_LIST);
			if (cidrsFromRedis != null && !cidrsFromRedis.isEmpty()) {
				synchronized (cidrCache) {
					cidrCache.clear();
					cidrCache.addAll(cidrsFromRedis);
				}
				log.info("[IdcIpBlockService] Redis에서 캐시 로드 완료 - {} 개 대역", cidrsFromRedis.size());
			} else {
				log.warn("[IdcIpBlockService] Redis에 저장된 IDC IP 리스트가 없음 - 리스트 갱신 필요");
			}
		} catch (Exception e) {
			log.error("[IdcIpBlockService] Redis 캐시 로드 실패 - 오류: {}", e.getMessage());
		}
	}

	/**
	 * IP가 CIDR 대역에 포함되는지 확인
	 *
	 * @param ip   클라이언트 IP
	 * @param cidr CIDR 대역 (예: "13.34.0.0/16")
	 * @return 포함 여부
	 */
	private boolean isIpInCidr(String ip, String cidr) {
		try {
			// Apache Commons Net SubnetUtils 사용
			SubnetUtils subnet = new SubnetUtils(cidr);
			subnet.setInclusiveHostCount(true); // 네트워크 주소와 브로드캐스트 주소도 포함
			return subnet.getInfo().isInRange(ip);
		} catch (IllegalArgumentException e) {
			// CIDR 형식이 잘못된 경우
			log.warn("[IdcIpBlockService] 잘못된 CIDR 형식 - CIDR: {}, 오류: {}", cidr, e.getMessage());
			return false;
		} catch (Exception e) {
			// IP 형식이 잘못된 경우
			return false;
		}
	}

	/**
	 * 현재 캐시된 IDC IP 대역 개수 반환 (모니터링용)
	 *
	 * @return 캐시된 CIDR 대역 개수
	 */
	public int getCachedCidrCount() {
		synchronized (cidrCache) {
			return cidrCache.size();
		}
	}
}
