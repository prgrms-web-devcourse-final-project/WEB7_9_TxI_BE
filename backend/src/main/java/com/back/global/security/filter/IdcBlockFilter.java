package com.back.global.security.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.error.code.SecurityErrorCode;
import com.back.global.response.ApiResponse;
import com.back.global.security.service.IdcIpBlockService;
import com.back.global.security.util.ClientIpResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IDC(데이터센터) IP 차단 필터
 *
 * 목적:
 * - AWS, Azure, GCP 등 클라우드 데이터센터에서 오는 요청 차단
 * - 서버 매크로 원천 봉쇄
 *
 * 우선순위:
 * - WhitelistFilter 이후, RateLimitFilter 이전
 *
 * 동작 방식:
 * 1. 화이트리스트 IP는 건너뜀
 * 2. 클라이언트 IP 추출
 * 3. IDC IP 대역에 포함되는지 확인
 * 4. 포함되면 즉시 403 Forbidden 응답
 *
 * 예외 처리:
 * - IDC IP 차단 시 사용자에게 VPN/프록시 해제 안내
 * - 정상 사용자가 VPN 사용 시 차단될 수 있음을 메시지로 안내
 *
 * 로깅:
 * - 차단된 IP, 요청 URI, 타임스탬프 기록
 */
@Slf4j
@Profile("!test")
@Component
@ConditionalOnProperty(name = "security.bot-protection.idc-block.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class IdcBlockFilter extends OncePerRequestFilter {

	private final ClientIpResolver clientIpResolver;
	private final IdcIpBlockService idcIpBlockService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		// 화이트리스트 IP는 건너뜀
		if (WhitelistFilter.isWhitelistedRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = clientIpResolver.getClientIp(request);
		String requestUri = request.getRequestURI();

		// AWS ALB 헬스체크는 무조건 통과 (서버 무한 재부팅 방지)
		if (isHealthCheckRequest(request, requestUri)) {
			log.debug("[IdcBlockFilter] 헬스체크 요청 통과 - IP: {}, URI: {}", clientIp, requestUri);
			filterChain.doFilter(request, response);
			return;
		}

		// IDC IP 대역 확인
		if (idcIpBlockService.isIdcIp(clientIp)) {
			log.warn("[IdcBlockFilter] IDC IP 차단 - IP: {}, URI: {}, Method: {}",
				clientIp, requestUri, request.getMethod());

			// 403 Forbidden 응답
			sendForbiddenResponse(response, clientIp);
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * AWS ALB/ELB 헬스체크 요청인지 확인
	 *
	 * 헬스체크 패턴:
	 * - User-Agent: ELB-HealthChecker/2.0
	 * - URI: /, /actuator/health, /health
	 *
	 * @param request HTTP 요청
	 * @param requestUri 요청 URI
	 * @return 헬스체크 여부
	 */
	private boolean isHealthCheckRequest(HttpServletRequest request, String requestUri) {
		// User-Agent 체크
		String userAgent = request.getHeader("User-Agent");
		if (userAgent != null && userAgent.startsWith("ELB-HealthChecker")) {
			return true;
		}

		// 헬스체크 경로 체크
		return "/".equals(requestUri) ||
			"/actuator/health".equals(requestUri) ||
			"/health".equals(requestUri);
	}

	/**
	 * 403 Forbidden 응답 전송
	 *
	 * @param response HTTP 응답
	 * @param clientIp 클라이언트 IP
	 */
	private void sendForbiddenResponse(HttpServletResponse response, String clientIp) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<?> apiResponse = ApiResponse.fail(SecurityErrorCode.IDC_IP_BLOCKED);

		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
		response.getWriter().flush();
	}
}
