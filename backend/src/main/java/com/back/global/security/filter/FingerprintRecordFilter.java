package com.back.global.security.filter;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.security.service.FingerprintService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fingerprint 기록 필터
 *
 * 목적:
 * - 모든 SMS API 응답을 가로채서 성공/실패 여부를 Fingerprint에 기록
 * - 컨트롤러에 도달하기 전에 발생하는 Validation 오류(400)도 추적
 *
 * 우선순위:
 * - 일반 Servlet Filter로 등록 (Spring Security 필터 체인 외부)
 * - 모든 요청/응답을 감싸서 최종 상태 코드를 확인 가능
 *
 * 동작 방식:
 * 1. SMS API 경로 요청인지 확인
 * 2. X-Device-Id 헤더에서 visitorId 추출
 * 3. 요청 처리 후 HTTP 상태 코드 확인
 *    - 200-299: 성공으로 기록
 *    - 그 외: 실패로 기록 (400 Validation Error 포함)
 * 4. FingerprintService.recordAttempt() 호출
 *
 * 중요:
 * - Validation 오류(400), 서버 오류(500) 모두 실패로 간주
 * - Rate Limit 차단(429)은 RateLimitFilter에서 이미 기록됨 (중복 방지)
 */
@Slf4j
@Profile("!test")
@Component
@ConditionalOnBean(FingerprintService.class)
@RequiredArgsConstructor
public class FingerprintRecordFilter extends OncePerRequestFilter {

	private static final String HEADER_DEVICE_ID = "X-Device-Id";

	private final FingerprintService fingerprintService;

	// SMS 관련 경로
	private static final String SMS_SEND_PATH = "/api/v1/sms/send";
	private static final String SMS_VERIFY_PATH = "/api/v1/sms/verify";
	private static final String PRE_REGISTER_PATH = "/api/v1/events/";
	private static final String PRE_REGISTER_SUFFIX = "/pre-registers";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		String visitorId = request.getHeader(HEADER_DEVICE_ID);

		// SMS API 경로가 아니거나 visitorId가 없으면 기록하지 않음
		if (!isSmsOrPreRegisterPath(requestUri) || visitorId == null || visitorId.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			// 요청 처리
			filterChain.doFilter(request, response);
		} finally {
			// 응답 상태 코드로 성공/실패 판단 (finally 블록에서 항상 실행)
			int status = response.getStatus();

			// 429 Too Many Requests는 RateLimitFilter에서 이미 기록했으므로 건너뜀 (중복 방지)
			if (status == 429) {
				return;
			}

			boolean success = (status >= 200 && status < 300);

			// Fingerprint 기록
			fingerprintService.recordAttempt(visitorId, success);
		}
	}

	/**
	 * SMS 또는 사전등록 경로인지 확인
	 *
	 * @param requestUri 요청 URI
	 * @return SMS/사전등록 경로 여부
	 */
	private boolean isSmsOrPreRegisterPath(String requestUri) {
		return requestUri.equals(SMS_SEND_PATH) ||
			requestUri.equals(SMS_VERIFY_PATH) ||
			(requestUri.startsWith(PRE_REGISTER_PATH) && requestUri.endsWith(PRE_REGISTER_SUFFIX));
	}

	/**
	 * FilterRegistrationBean 설정
	 * - 일반 Servlet Filter로 등록하여 Spring Security 필터 체인 이후에 실행
	 * - 모든 응답 코드를 캡처할 수 있도록 최대한 늦게 실행
	 */
	@Configuration
	@Profile("!test")
	@ConditionalOnBean(FingerprintService.class)
	public static class FingerprintRecordFilterConfig {

		@Bean
		public FilterRegistrationBean<FingerprintRecordFilter> fingerprintRecordFilterRegistration(
			FingerprintRecordFilter filter
		) {
			FilterRegistrationBean<FingerprintRecordFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setOrder(Ordered.LOWEST_PRECEDENCE); // 가장 늦게 실행
			return registration;
		}
	}
}
