package com.back.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.back.global.http.RequestMetaProvider;

@TestConfiguration
public class TestRequestMetaConfig {
	@Bean
	public RequestMetaProvider requestMetaProvider() {
		return new RequestMetaProvider() {
			@Override
			public String userAgent() {
				return "test-agent";
			}

			@Override
			public String clientIp() {
				return "127.0.0.1";
			}
		};
	}
}
