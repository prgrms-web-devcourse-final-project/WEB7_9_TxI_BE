package com.back.global.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.global.error.code.SecurityErrorCode;
import com.back.global.response.ApiResponse;
import com.back.global.security.service.FingerprintService;
import com.back.global.security.service.RateLimitService;
import com.back.global.security.util.CachedBodyHttpServletRequest;
import com.back.global.security.util.ClientIpResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate Limiting 필터 (SMS 전용)
 *
 * 목적:
 * - SMS API 비용 발생 방지
 * - 무한 요청 봇 차단
 *
 * 우선순위:
 * - WhitelistFilter, IdcBlockFilter 이후
 * - FingerprintFilter 이전
 *
 * 동작 방식:
 * 1. 화이트리스트 IP는 건너뜀
 * 2. SMS 경로만 Rate Limit 적용 (사전등록은 제외)
 * 3. IP + 전화번호 조합으로 1분당 7회 제한
 * 4. 초과 시 429 Too Many Requests 응답
 *
 * 예외 처리:
 * - Redis 장애 시 요청 허용 (가용성 우선)
 * - 로그만 남기고 통과
 *
 * SMS Rate Limit 특이사항:
 * - 전화번호는 JSON request body에서 추출
 * - CachedBodyHttpServletRequest로 body 재사용 가능하게 처리
 * - 없을 경우 IP만으로 제한
 */
@Slf4j
@Profile("!test")
@Component
@ConditionalOnBean(RateLimitService.class)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String HEADER_DEVICE_ID = "X-Device-Id";

	private final ClientIpResolver clientIpResolver;
	private final RateLimitService rateLimitService;
	private final ObjectMapper objectMapper;
	private FingerprintService fingerprintService;

	@Autowired(required = false)
	public void setFingerprintService(FingerprintService fingerprintService) {
		this.fingerprintService = fingerprintService;
	}

	// SMS/사전등록 관련 경로
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

		// 화이트리스트 IP는 건너뜀
		if (WhitelistFilter.isWhitelistedRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String requestUri = request.getRequestURI();

		// SMS 경로가 아니면 통과 (Rate Limit 미적용)
		if (!isSmsOrPreRegisterPath(requestUri)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = clientIpResolver.getClientIp(request);

		// CachedBodyHttpServletRequest로 wrapping하여 body 재사용 가능하게 함
		CachedBodyHttpServletRequest wrappedRequest;
		try {
			wrappedRequest = new CachedBodyHttpServletRequest(request);
		} catch (IOException e) {
			// Request Body 크기 제한 초과 (DoS 공격 방지)
			log.warn("[RateLimitFilter] Request Body 크기 제한 초과 - IP: {}, URI: {}, 오류: {}",
				clientIp, requestUri, e.getMessage());
			sendTooManyRequestsResponse(response, SecurityErrorCode.TOO_MANY_REQUESTS);
			return;
		}

		// SMS/사전등록 Rate Limit 체크
		String phoneNumber = extractPhoneNumber(wrappedRequest);
		boolean allowed = rateLimitService.allowSmsRequest(clientIp, phoneNumber);

		if (!allowed) {
			log.warn("[RateLimitFilter] SMS Rate Limit 초과 - IP: {}, Phone: {}, URI: {}",
				clientIp, maskPhoneNumber(phoneNumber), requestUri);

			// Fingerprint 실패 기록 (Rate Limit 차단도 실패로 간주) - 이벤트별, 액션별
			String visitorId = request.getHeader(HEADER_DEVICE_ID);
			if (fingerprintService != null && visitorId != null) {
				Long eventId = extractEventId(requestUri, wrappedRequest);
				String action = getActionTypeFromUri(requestUri);
				fingerprintService.recordAttempt(visitorId, eventId, action, false);
			}

			sendTooManyRequestsResponse(response, SecurityErrorCode.TOO_MANY_SMS_REQUESTS);
			return;
		}

		// wrappedRequest로 전달하여 Controller에서도 body 읽을 수 있게 함
		filterChain.doFilter(wrappedRequest, response);
	}

	/**
	 * SMS 경로인지 확인
	 *
	 * @param requestUri 요청 URI
	 * @return SMS 경로 여부
	 */
	private boolean isSmsOrPreRegisterPath(String requestUri) {
		return requestUri.equals(SMS_SEND_PATH) ||
			requestUri.equals(SMS_VERIFY_PATH);
	}

	/**
	 * SMS 요청에서 eventId 추출
	 *
	 * @param requestUri 요청 URI
	 * @param request HTTP 요청
	 * @return eventId (추출 실패 시 null)
	 */
	private Long extractEventId(String requestUri, CachedBodyHttpServletRequest request) {
		// SMS 경로: body에서 eventId 추출
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
			log.warn("[RateLimitFilter] Request body에서 eventId 추출 실패 - 오류: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * 요청 URI에서 액션 타입 추출
	 *
	 * @param requestUri 요청 URI
	 * @return 액션 타입 (sms_send, sms_verify, pre_register)
	 */
	private String getActionTypeFromUri(String requestUri) {
		if (requestUri.equals(SMS_SEND_PATH)) {
			return FingerprintService.ACTION_SMS_SEND;
		} else if (requestUri.equals(SMS_VERIFY_PATH)) {
			return FingerprintService.ACTION_SMS_VERIFY;
		} else {
			return FingerprintService.ACTION_PRE_REGISTER;
		}
	}

	/**
	 * Request에서 전화번호 추출
	 *
	 * 우선순위:
	 * 1. Query Parameter (phoneNumber)
	 * 2. JSON Request Body (phoneNumber 필드)
	 *
	 * @param request HTTP 요청 (CachedBodyHttpServletRequest)
	 * @return 전화번호 (없으면 null)
	 */
	private String extractPhoneNumber(CachedBodyHttpServletRequest request) {
		// 1. Query Parameter에서 추출
		String phoneNumber = request.getParameter("phoneNumber");
		if (phoneNumber != null) {
			return phoneNumber;
		}

		// 2. JSON Request Body에서 추출
		try {
			byte[] content = request.getCachedBody();
			if (content.length > 0) {
				String body = new String(content, StandardCharsets.UTF_8);
				JsonNode jsonNode = objectMapper.readTree(body);

				// phoneNumber 필드가 있으면 추출
				if (jsonNode.has("phoneNumber")) {
					return jsonNode.get("phoneNumber").asText();
				}
			}
		} catch (Exception e) {
			// JSON 파싱 실패 시 무시 (null 반환)
		}

		// 전화번호를 찾지 못한 경우 null 반환 (IP만으로 제한)
		return null;
	}

	/**
	 * 전화번호 마스킹 (로그용)
	 *
	 * @param phoneNumber 전화번호
	 * @return 마스킹된 전화번호
	 */
	private String maskPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 8) {
			return "****";
		}
		return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
	}

	/**
	 * 429 Too Many Requests 응답 전송
	 *
	 * @param response  HTTP 응답
	 * @param errorCode 에러 코드
	 */
	private void sendTooManyRequestsResponse(HttpServletResponse response,
		SecurityErrorCode errorCode) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<?> apiResponse = ApiResponse.fail(errorCode);

		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
		response.getWriter().flush();
	}
}
