package com.back.api.payment.payment.config;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TossPaymentConfig {

	@Value("${toss.payments.secret}")
	private String secretKey;

	@Bean
	public RestClient tossRestClient() {
		String encodedKey = Base64.getEncoder()
			.encodeToString((secretKey + ":").getBytes());

		return RestClient.builder()
			.baseUrl("https://api.tosspayments.com")
			.defaultHeader("Authorization", "Basic " + encodedKey)
			.defaultHeader("Content-Type", "application/json")
			.build();
	}

}
