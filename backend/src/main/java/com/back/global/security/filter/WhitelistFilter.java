package com.back.global.security.filter;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.properties.SecurityProperties;
import com.back.global.security.util.ClientIpResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IP 화이트리스트 필터
 *
 * 목적:
 * - 개발/테스트 환경에서 특정 IP를 화이트리스트에 등록하여 모든 보안 필터 우회
 * - Rate Limit, IDC 차단, Fingerprint 검증 등을 건너뜀
 *
 * 우선순위:
 * - 가장 먼저 실행되어야 함 (최상단 필터)
 *
 * 동작 방식:
 * 1. 클라이언트 IP 추출
 * 2. 화이트리스트에 포함되어 있는지 확인
 * 3. 포함되어 있으면 request attribute에 "WHITELISTED_IP" 플래그 설정
 * 4. 이후 필터들은 이 플래그를 확인하여 검증 건너뜀
 *
 * 주의사항:
 * - 프로덕션 환경에서는 화이트리스트를 비워두거나 신뢰할 수 있는 IP만 등록
 * - 잘못된 IP 등록 시 보안 우회 위험
 */
@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class WhitelistFilter extends OncePerRequestFilter {

	public static final String WHITELISTED_IP_ATTRIBUTE = "WHITELISTED_IP";

	private final ClientIpResolver clientIpResolver;
	private final SecurityProperties securityProperties;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String clientIp = clientIpResolver.getClientIp(request);

		// 화이트리스트 확인
		if (isWhitelisted(clientIp)) {
			request.setAttribute(WHITELISTED_IP_ATTRIBUTE, true);
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * IP가 화이트리스트에 포함되어 있는지 확인
	 *
	 * @param ip 클라이언트 IP
	 * @return 화이트리스트 포함 여부
	 */
	private boolean isWhitelisted(String ip) {
		if (securityProperties.getWhitelistIps() == null || securityProperties.getWhitelistIps().isEmpty()) {
			return false;
		}

		// 정확히 일치하는 IP 또는 CIDR 대역 확인
		for (String whitelistIp : securityProperties.getWhitelistIps()) {
			if (whitelistIp.equals(ip)) {
				return true;
			}
			// TODO: CIDR 대역 매칭 추가 (예: 192.168.0.0/16)
			// 현재는 정확한 IP 매칭만 지원
		}

		return false;
	}

	/**
	 * Request가 화이트리스트 IP인지 확인하는 헬퍼 메서드
	 * 다른 필터에서 사용 가능
	 *
	 * @param request HTTP 요청
	 * @return 화이트리스트 여부
	 */
	public static boolean isWhitelistedRequest(HttpServletRequest request) {
		Boolean whitelisted = (Boolean)request.getAttribute(WHITELISTED_IP_ATTRIBUTE);
		return Boolean.TRUE.equals(whitelisted);
	}
}
