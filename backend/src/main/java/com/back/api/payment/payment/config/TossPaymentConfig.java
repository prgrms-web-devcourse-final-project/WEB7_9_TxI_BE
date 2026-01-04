package com.back.api.payment.payment.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Toss Payments API RestClient 설정
 * 타임아웃 설정:
 * - 연결 타임아웃: 3초 - 서버 연결까지 대기 시간
 * - 읽기 타임아웃: 5초 - 응답 대기 시간 (Toss 권장 최대 30초이나 UX 고려)
 * 타임아웃 초과 시 ResourceAccessException 발생 → 서킷브레이커에서 처리
 */
@Configuration
public class TossPaymentConfig {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

	@Value("${toss.payments.secret}")
	private String secretKey;

	@Bean
	public RestClient tossRestClient() {
		String encodedKey = Base64.getEncoder()
			.encodeToString((secretKey + ":").getBytes());

		// JDK HttpClient 기반 타임아웃 설정
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build();

		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(READ_TIMEOUT);

		return RestClient.builder()
			.baseUrl("https://api.tosspayments.com")
			.defaultHeader("Authorization", "Basic " + encodedKey)
			.defaultHeader("Content-Type", "application/json")
			.requestFactory(requestFactory)
			.build();
	}
}
