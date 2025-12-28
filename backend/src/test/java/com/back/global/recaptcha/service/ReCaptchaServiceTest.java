package com.back.global.recaptcha.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.recaptcha.config.ReCaptchaProperties;
import com.back.global.recaptcha.dto.ReCaptchaResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReCaptchaService 단위 테스트")
class ReCaptchaServiceTest {

	@Mock
	private ReCaptchaProperties reCaptchaProperties;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private ReCaptchaService reCaptchaService;

	private static final String TEST_TOKEN = "test-recaptcha-token";
	private static final String TEST_IP = "127.0.0.1";
	private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
	private static final String SECRET_KEY = "test-secret-key";

	@BeforeEach
	void setUp() {
		lenient().when(reCaptchaProperties.getVerifyUrl()).thenReturn(VERIFY_URL);
		lenient().when(reCaptchaProperties.getSecretKey()).thenReturn(SECRET_KEY);
	}

	@Nested
	@DisplayName("토큰 검증 성공 케이스")
	class VerifyTokenSuccess {

		@Test
		@DisplayName("유효한 토큰으로 검증 성공")
		void verifyTokenSuccess() {
			// given
			ReCaptchaResponse mockResponse = new ReCaptchaResponse();
			mockResponse.setSuccess(true);
			mockResponse.setScore(0.9);
			mockResponse.setAction("pre_register");

			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(mockResponse);

			// when & then
			assertThatCode(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.doesNotThrowAnyException();

			verify(restTemplate, times(1)).postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			);
		}

		@Test
		@DisplayName("점수가 0.5인 경우 검증 성공")
		void verifyTokenWithMinimumScore() {
			// given
			ReCaptchaResponse mockResponse = new ReCaptchaResponse();
			mockResponse.setSuccess(true);
			mockResponse.setScore(0.5);

			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(mockResponse);

			// when & then
			assertThatCode(() -> reCaptchaService.verifyToken(TEST_TOKEN, null))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("토큰 검증 실패 케이스")
	class VerifyTokenFailure {

		@Test
		@DisplayName("토큰이 null인 경우 예외 발생")
		void verifyTokenFailWhenTokenIsNull() {
			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(null, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_TOKEN_MISSING);

			verify(restTemplate, never()).postForObject(anyString(), any(), any());
		}

		@Test
		@DisplayName("토큰이 빈 문자열인 경우 예외 발생")
		void verifyTokenFailWhenTokenIsBlank() {
			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken("", TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_TOKEN_MISSING);

			verify(restTemplate, never()).postForObject(anyString(), any(), any());
		}

		@Test
		@DisplayName("Google API 호출 실패 시 예외 발생")
		void verifyTokenFailWhenApiCallFails() {
			// given
			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenThrow(new RestClientException("API call failed"));

			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}

		@Test
		@DisplayName("Google API 응답이 null인 경우 예외 발생")
		void verifyTokenFailWhenResponseIsNull() {
			// given
			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(null);

			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}

		@Test
		@DisplayName("success가 false인 경우 예외 발생")
		void verifyTokenFailWhenSuccessIsFalse() {
			// given
			ReCaptchaResponse mockResponse = new ReCaptchaResponse();
			mockResponse.setSuccess(false);

			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(mockResponse);

			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_VERIFICATION_FAILED);
		}

		@Test
		@DisplayName("점수가 0.5 미만인 경우 예외 발생")
		void verifyTokenFailWhenScoreTooLow() {
			// given
			ReCaptchaResponse mockResponse = new ReCaptchaResponse();
			mockResponse.setSuccess(true);
			mockResponse.setScore(0.49);

			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(mockResponse);

			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_SCORE_TOO_LOW);
		}

		@Test
		@DisplayName("점수가 null인 경우 예외 발생")
		void verifyTokenFailWhenScoreIsNull() {
			// given
			ReCaptchaResponse mockResponse = new ReCaptchaResponse();
			mockResponse.setSuccess(true);
			mockResponse.setScore(null);

			when(restTemplate.postForObject(
				eq(VERIFY_URL),
				any(HttpEntity.class),
				eq(ReCaptchaResponse.class)
			)).thenReturn(mockResponse);

			// when & then
			assertThatThrownBy(() -> reCaptchaService.verifyToken(TEST_TOKEN, TEST_IP))
				.isInstanceOf(ErrorException.class)
				.hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RECAPTCHA_SCORE_TOO_LOW);
		}
	}
}
