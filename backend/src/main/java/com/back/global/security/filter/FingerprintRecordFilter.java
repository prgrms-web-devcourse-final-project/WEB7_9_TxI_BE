package com.back.global.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.security.service.FingerprintService;
import com.back.global.security.util.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private final ObjectMapper objectMapper;

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

		// SMS API 또는 사전등록 경로가 아니거나 visitorId가 없으면 기록하지 않음
		if (!isSmsOrPreRegisterPath(requestUri) || visitorId == null || visitorId.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		// SMS 경로는 body에서 eventId를 추출해야 하므로 CachedBodyHttpServletRequest로 wrapping
		// (단, RateLimitFilter에서 이미 wrapping된 경우 재사용)
		HttpServletRequest processedRequest = request;
		if (isSmsPath(requestUri)) {
			if (request instanceof CachedBodyHttpServletRequest) {
				// 이미 wrapping된 경우 재사용
				processedRequest = request;
			} else {
				// wrapping 필요
				try {
					processedRequest = new CachedBodyHttpServletRequest(request);
				} catch (IOException e) {
					log.error("[FingerprintRecordFilter] Request body 캐싱 실패 - URI: {}, 오류: {}", requestUri, e.getMessage());
					filterChain.doFilter(request, response);
					return;
				}
			}
		}

		// eventId 추출 (사전등록: URI에서, SMS: body에서)
		Long eventId = extractEventId(requestUri, processedRequest);

		// 액션 타입 결정
		String action = getActionType(requestUri);

		log.info("[FingerprintRecordFilter] 요청 처리 - URI: {}, visitorId: {}, eventId: {}, action: {}",
			requestUri, visitorId, eventId, action);

		try {
			// 요청 처리
			filterChain.doFilter(processedRequest, response);
		} finally {
			// 응답 상태 코드로 성공/실패 판단 (finally 블록에서 항상 실행)
			int status = response.getStatus();

			// 429 Too Many Requests는 RateLimitFilter에서 이미 기록했으므로 건너뜀 (중복 방지)
			if (status == 429) {
				return;
			}

			boolean success = (status >= 200 && status < 300);

			log.info("[FingerprintRecordFilter] Fingerprint 기록 - visitorId: {}, eventId: {}, action: {}, status: {}, success: {}",
				visitorId, eventId, action, status, success);

			// Fingerprint 기록 (이벤트별, 액션별)
			fingerprintService.recordAttempt(visitorId, eventId, action, success);
		}
	}

	/**
	 * SMS 또는 사전등록 경로인지 확인
	 *
	 * @param requestUri 요청 URI
	 * @return SMS/사전등록 경로 여부
	 */
	private boolean isSmsOrPreRegisterPath(String requestUri) {
		return isSmsPath(requestUri) ||
			(requestUri.startsWith(PRE_REGISTER_PATH) && requestUri.endsWith(PRE_REGISTER_SUFFIX));
	}

	/**
	 * SMS 경로인지 확인
	 *
	 * @param requestUri 요청 URI
	 * @return SMS 경로 여부
	 */
	private boolean isSmsPath(String requestUri) {
		return requestUri.equals(SMS_SEND_PATH) || requestUri.equals(SMS_VERIFY_PATH);
	}

	/**
	 * 요청에서 eventId 추출
	 *
	 * @param requestUri 요청 URI
	 * @param request HTTP 요청
	 * @return eventId (추출 실패 시 null)
	 */
	private Long extractEventId(String requestUri, HttpServletRequest request) {
		// 사전등록 경로: /api/v1/events/{eventId}/pre-registers (URI에서 추출)
		if (requestUri.startsWith(PRE_REGISTER_PATH) && requestUri.endsWith(PRE_REGISTER_SUFFIX)) {
			try {
				String eventIdStr = requestUri
					.substring(PRE_REGISTER_PATH.length())
					.replace(PRE_REGISTER_SUFFIX, "");
				return Long.parseLong(eventIdStr);
			} catch (NumberFormatException e) {
				log.warn("[FingerprintRecordFilter] eventId 추출 실패 - URI: {}", requestUri);
				return null;
			}
		}

		// SMS 경로: body에서 eventId 추출
		if (isSmsPath(requestUri) && request instanceof CachedBodyHttpServletRequest) {
			CachedBodyHttpServletRequest cachedRequest = (CachedBodyHttpServletRequest) request;
			return extractEventIdFromBody(cachedRequest);
		}

		return null;
	}

	/**
	 * Request Body에서 eventId 추출
	 *
	 * @param request CachedBodyHttpServletRequest
	 * @return eventId (추출 실패 시 null)
	 */
	private Long extractEventIdFromBody(CachedBodyHttpServletRequest request) {
		try {
			byte[] content = request.getCachedBody();
			if (content.length > 0) {
				String body = new String(content, StandardCharsets.UTF_8);
				JsonNode jsonNode = objectMapper.readTree(body);

				if (jsonNode.has("eventId")) {
					return jsonNode.get("eventId").asLong();
				}
			}
		} catch (Exception e) {
			log.warn("[FingerprintRecordFilter] Request body에서 eventId 추출 실패 - 오류: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * 요청 URI에서 액션 타입 추출
	 *
	 * @param requestUri 요청 URI
	 * @return 액션 타입 (sms_send, sms_verify, pre_register)
	 */
	private String getActionType(String requestUri) {
		if (requestUri.equals(SMS_SEND_PATH)) {
			return FingerprintService.ACTION_SMS_SEND;
		} else if (requestUri.equals(SMS_VERIFY_PATH)) {
			return FingerprintService.ACTION_SMS_VERIFY;
		} else {
			return FingerprintService.ACTION_PRE_REGISTER;
		}
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
