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
import com.back.global.security.service.FingerprintService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Device Fingerprint 검증 필터
 *
 * 목적:
 * - IP를 바꿔가며 접속하는 업자 탐지
 * - FingerprintJS를 통해 브라우저/디바이스 고유 ID 추적
 *
 * 우선순위:
 * - WhitelistFilter, IdcBlockFilter, RateLimitFilter 이후
 * - Controller 진입 전
 *
 * 동작 방식:
 * 1. 화이트리스트 IP는 건너뜀
 * 2. Request Header에서 X-Device-Id 추출 (FingerprintJS visitorId)
 * 3. visitorId별 시도 횟수, 실패율 확인
 * 4. 차단 기준 초과 시 400 Bad Request 응답
 *
 * 헤더:
 * - X-Device-Id: FingerprintJS visitorId
 *
 * 차단 기준:
 * - 전체 시도 >= 5회
 * - 실패율 >= 80%
 *
 * 예외 처리:
 * - X-Device-Id 없는 요청: 1차 허용 (IP Rate Limit으로만 통제)
 * - Redis 장애 시: 요청 허용 (가용성 우선)
 *
 * 주의사항:
 * - 이 필터는 검증만 수행 (기록은 Controller/Service에서)
 * - 사전등록 성공/실패 시 FingerprintService.recordAttempt() 호출 필요
 */
@Slf4j
@Profile("!test")
@Component
@ConditionalOnProperty(name = "security.bot-protection.fingerprint.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class FingerprintFilter extends OncePerRequestFilter {

	private static final String HEADER_DEVICE_ID = "X-Device-Id";

	private final FingerprintService fingerprintService;
	private final ObjectMapper objectMapper;

	// 보안 체크 대상 경로
	private static final String PRE_REGISTER_PATH = "/api/v1/events/";
	private static final String PRE_REGISTER_SUFFIX = "/pre-registers";
	private static final String SMS_PATH = "/api/v1/sms/";

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

		String requestUri = request.getRequestURI();

		// 사전등록 또는 SMS 경로 체크
		if (isTargetPath(requestUri)) {
			String visitorId = request.getHeader(HEADER_DEVICE_ID);

			// Fingerprint 검증
			boolean allowed = fingerprintService.validateFingerprint(visitorId);

			if (!allowed) {
				log.warn("[FingerprintFilter] Fingerprint 차단 - visitorId: {}, URI: {}",
					visitorId, requestUri);
				sendBadRequestResponse(response);
				return;
			}

			// visitorId를 request attribute에 저장 (Service에서 사용)
			if (visitorId != null && !visitorId.isEmpty()) {
				request.setAttribute("VISITOR_ID", visitorId);
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Fingerprint 체크 대상 경로인지 확인
	 *
	 * @param requestUri 요청 URI
	 * @return 대상 경로 여부
	 */
	private boolean isTargetPath(String requestUri) {
		// 사전등록 경로
		boolean isPreRegister = requestUri.startsWith(PRE_REGISTER_PATH) && requestUri.endsWith(PRE_REGISTER_SUFFIX);

		// SMS 경로 (/api/v1/sms/send, /api/v1/sms/verify)
		boolean isSms = requestUri.startsWith(SMS_PATH);

		return isPreRegister || isSms;
	}

	/**
	 * 400 Bad Request 응답 전송
	 *
	 * @param response HTTP 응답
	 */
	private void sendBadRequestResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<?> apiResponse = ApiResponse.fail(SecurityErrorCode.SUSPICIOUS_ACTIVITY);

		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
		response.getWriter().flush();
	}
}
