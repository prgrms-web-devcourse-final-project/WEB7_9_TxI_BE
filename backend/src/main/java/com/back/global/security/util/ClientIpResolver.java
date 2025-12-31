package com.back.global.security.util;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.global.properties.SecurityProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 클라이언트의 실제 IP 주소를 추출하는 유틸리티 클래스
 *
 * AWS ALB/CloudFront 환경에서 X-Forwarded-For 헤더를 통해 실제 클라이언트 IP를 획득
 *
 * 보안 고려사항:
 * - X-Forwarded-For는 클라이언트가 조작 가능하므로 신뢰할 수 있는 프록시만 고려
 * - trustedProxyCount 설정에 따라 안전한 IP 추출
 * - 0: 직접 연결 (RemoteAddr만 사용)
 * - 1: ALB만 있음 (마지막 IP = ALB가 추가한 실제 클라이언트 IP)
 * - 2: CloudFront + ALB (마지막에서 2번째 IP = CloudFront가 본 클라이언트 IP)
 */
@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

	private final SecurityProperties securityProperties;

	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String HEADER_X_REAL_IP = "X-Real-IP";
	private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
	private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
	private static final String UNKNOWN = "unknown";

	/**
	 * HTTP 요청에서 실제 클라이언트 IP 주소 추출
	 *
	 * 우선순위:
	 * 1. X-Forwarded-For (ALB/CloudFront)
	 * 2. X-Real-IP (Nginx)
	 * 3. Proxy-Client-IP
	 * 4. WL-Proxy-Client-IP
	 * 5. request.getRemoteAddr()
	 *
	 * @param request HTTP 요청
	 * @return 클라이언트 IP 주소
	 */
	public String resolveClientIp(HttpServletRequest request) {
		String ip = getIpFromHeader(request, HEADER_X_FORWARDED_FOR);

		// X-Forwarded-For에서 IP를 찾지 못한 경우 다른 헤더 시도
		if (isInvalidIp(ip)) {
			ip = getIpFromHeader(request, HEADER_X_REAL_IP);
		}
		if (isInvalidIp(ip)) {
			ip = getIpFromHeader(request, HEADER_PROXY_CLIENT_IP);
		}
		if (isInvalidIp(ip)) {
			ip = getIpFromHeader(request, HEADER_WL_PROXY_CLIENT_IP);
		}

		// 모든 헤더에서 IP를 찾지 못한 경우 RemoteAddr 사용
		if (isInvalidIp(ip)) {
			ip = request.getRemoteAddr();
			log.debug("IP 추출: RemoteAddr 사용 - IP: {}", ip);
		}

		return ip;
	}

	/**
	 * 헤더에서 IP 추출 (보안 강화 버전)
	 * X-Forwarded-For의 경우 "client-ip, proxy1, proxy2, ALB-ip" 형태
	 *
	 * 보안 전략:
	 * - trustedProxyCount = 0: X-Forwarded-For 무시, RemoteAddr만 사용
	 * - trustedProxyCount = 1: 마지막 IP 사용 (ALB가 추가한 실제 클라이언트 IP)
	 * - trustedProxyCount = 2: 마지막에서 2번째 IP 사용 (CloudFront + ALB)
	 *
	 * 예시 (ALB만 있는 경우, trustedProxyCount=1):
	 * - 정상: "203.0.113.1, 10.0.0.5" -> 마지막 IP "10.0.0.5"는 ALB 내부 IP이므로 잘못된 설정
	 * - 정상: "203.0.113.1" -> ALB가 추가한 실제 클라이언트 IP
	 * - 공격: "1.2.3.4, 203.0.113.1" -> 마지막 IP "203.0.113.1"이 실제 클라이언트 IP (안전)
	 *
	 * @param request HTTP 요청
	 * @param headerName 헤더 이름
	 * @return IP 주소 (추출 실패 시 null)
	 */
	private String getIpFromHeader(HttpServletRequest request, String headerName) {
		String ip = request.getHeader(headerName);
		int trustedProxyCount = securityProperties.getTrustedProxyCount();

		if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
			// trustedProxyCount = 0이면 X-Forwarded-For를 신뢰하지 않음
			if (trustedProxyCount == 0) {
				log.debug("IP 추출: trustedProxyCount=0이므로 {} 헤더 무시", headerName);
				return null;
			}

			if (ip.contains(",")) {
				String[] ips = ip.split(",");

				// 신뢰할 수 있는 프록시를 제외한 가장 오른쪽 IP 선택
				// trustedProxyCount=1, ips=["fake", "real"] -> index=1 (마지막)
				// trustedProxyCount=2, ips=["fake", "real", "cf", "alb"] -> index=2 (마지막에서 2번째)
				int clientIpIndex = Math.max(0, ips.length - trustedProxyCount - 1);
				ip = ips[clientIpIndex].trim();

				log.debug("IP 추출: {} 헤더에서 index {} IP 사용 (trustedProxyCount={}) - IP: {}",
					headerName, clientIpIndex, trustedProxyCount, ip);
			} else {
				log.debug("IP 추출: {} 헤더 사용 (단일 IP) - IP: {}", headerName, ip);
			}
			return ip;
		}

		return null;
	}

	/**
	 * IP 주소 유효성 검증
	 *
	 * @param ip IP 주소
	 * @return 유효하지 않으면 true
	 */
	private boolean isInvalidIp(String ip) {
		return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
	}

	/**
	 * IPv6 로컬 주소(::1)를 IPv4(127.0.0.1)로 변환
	 *
	 * @param ip IP 주소
	 * @return 변환된 IP 주소
	 */
	public String normalizeIp(String ip) {
		if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
			return "127.0.0.1";
		}
		return ip;
	}

	/**
	 * 전체 IP 추출 및 정규화 프로세스
	 *
	 * @param request HTTP 요청
	 * @return 정규화된 클라이언트 IP 주소
	 */
	public String getClientIp(HttpServletRequest request) {
		String ip = resolveClientIp(request);
		return normalizeIp(ip);
	}
}
