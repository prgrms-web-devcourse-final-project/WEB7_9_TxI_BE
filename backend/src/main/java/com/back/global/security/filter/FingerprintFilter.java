package com.back.global.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.error.code.SecurityErrorCode;
import com.back.global.response.ApiResponse;
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
	private static final String SMS_SEND_PATH = "/api/v1/sms/send";
	private static final String SMS_VERIFY_PATH = "/api/v1/sms/verify";

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
						log.error("[FingerprintFilter] Request body 캐싱 실패 - URI: {}, 오류: {}", requestUri, e.getMessage());
						sendBadRequestResponse(response);
						return;
					}
				}
			}

			String visitorId = processedRequest.getHeader(HEADER_DEVICE_ID);

			// eventId 추출 (사전등록: URI에서, SMS: body에서)
			Long eventId = extractEventId(requestUri, processedRequest);

			// 액션 타입 결정
			String action = getActionType(requestUri);

			log.info("[FingerprintFilter] 검증 시작 - URI: {}, visitorId: {}, eventId: {}, action: {}",
				requestUri, visitorId, eventId, action);

			// Fingerprint 검증 (이벤트별, 액션별)
			boolean allowed = fingerprintService.validateFingerprint(visitorId, eventId, action);

			if (!allowed) {
				log.warn("[FingerprintFilter] Fingerprint 차단 - visitorId: {}, eventId: {}, action: {}, URI: {}",
					visitorId, eventId, action, requestUri);
				sendBadRequestResponse(response);
				return;
			}

			// visitorId를 request attribute에 저장 (Service에서 사용)
			if (visitorId != null && !visitorId.isEmpty()) {
				processedRequest.setAttribute("VISITOR_ID", visitorId);
			}

			filterChain.doFilter(processedRequest, response);
			return;
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
		boolean isSms = isSmsPath(requestUri);

		return isPreRegister || isSms;
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
				log.warn("[FingerprintFilter] eventId 추출 실패 - URI: {}", requestUri);
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
			log.warn("[FingerprintFilter] Request body에서 eventId 추출 실패 - 오류: {}", e.getMessage());
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
