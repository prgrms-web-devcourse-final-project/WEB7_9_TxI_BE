package com.back.global.recaptcha.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.recaptcha.config.ReCaptchaProperties;
import com.back.global.recaptcha.dto.ReCaptchaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReCaptchaService {

	private final ReCaptchaProperties reCaptchaProperties;
	private final RestTemplate restTemplate;

	private static final double MINIMUM_SCORE = 0.5;

	/**
	 * reCAPTCHA v3 토큰 검증
	 *
	 * @param token reCAPTCHA 토큰
	 * @param remoteIp 클라이언트 IP
	 */
	public void verifyToken(String token, String remoteIp) {
		if (token == null || token.isBlank()) {
			log.warn("reCAPTCHA 토큰이 누락되었습니다.");
			throw new ErrorException(CommonErrorCode.RECAPTCHA_TOKEN_MISSING);
		}

		ReCaptchaResponse response = sendVerificationRequest(token, remoteIp);

		validateResponse(response);
	}

	/**
	 * Google reCAPTCHA API로 검증 요청 전송
	 */
	private ReCaptchaResponse sendVerificationRequest(String token, String remoteIp) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("secret", reCaptchaProperties.getSecretKey());
			params.add("response", token);
			if (remoteIp != null && !remoteIp.isBlank()) {
				params.add("remoteip", remoteIp);
			}

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			ReCaptchaResponse response = restTemplate.postForObject(
				reCaptchaProperties.getVerifyUrl(),
				request,
				ReCaptchaResponse.class
			);

			log.debug("reCAPTCHA 검증 응답: success={}, score={}, action={}, errorCodes={}",
				response != null ? response.isSuccess() : null,
				response != null ? response.getScore() : null,
				response != null ? response.getAction() : null,
				response != null ? response.getErrorCodes() : null
			);

			return response;

		} catch (RestClientException e) {
			log.error("reCAPTCHA API 호출 중 오류 발생", e);
			throw new ErrorException(CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}
	}

	/**
	 * reCAPTCHA 응답 검증
	 */
	private void validateResponse(ReCaptchaResponse response) {
		if (response == null) {
			log.error("reCAPTCHA 응답이 null입니다.");
			throw new ErrorException(CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}

		if (!response.isSuccess()) {
			log.warn("reCAPTCHA 검증 실패: {}", response.getErrorCodes());
			throw new ErrorException(CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}

		if (response.getScore() == null || response.getScore() < MINIMUM_SCORE) {
			log.warn("reCAPTCHA 점수가 너무 낮습니다: {} (최소: {})", response.getScore(), MINIMUM_SCORE);
			throw new ErrorException(CommonErrorCode.RECAPTCHA_SCORE_TOO_LOW);
		}

		log.info("reCAPTCHA 검증 성공: score={}, action={}", response.getScore(), response.getAction());
	}
}
