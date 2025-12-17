package com.back.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.global.properties.SmsProperties;
import com.back.global.services.sms.service.SmsService;
import com.back.global.services.sms.service.TestSmsService;
import com.back.global.services.sms.util.SmsUtilInterface;

import lombok.RequiredArgsConstructor;

/**
 * SMS 서비스 설정
 * 테스트 환경에서는 고정 인증번호를 사용하는 TestSmsService 제공
 */
@Configuration
@RequiredArgsConstructor
public class SmsConfig {

	private final SmsProperties smsProperties;

	/**
	 * 개발/테스트 환경용 - 고정 인증번호 사용
	 */
	@Bean
	@Primary
	@Profile({"dev", "test", "perf"})
	@ConditionalOnProperty(name = "sms.test.enabled", havingValue = "true")
	public SmsService testSmsService(
		SmsUtilInterface smsUtil,
		StringRedisTemplate redisTemplate
	) {
		return new TestSmsService(smsUtil, redisTemplate, smsProperties.getTest().getFixedCode());
	}

	/**
	 * 운영 환경용 - 랜덤 인증번호 생성
	 */
	@Bean
	@Profile("prod")
	public SmsService prodSmsService(
		SmsUtilInterface smsUtil,
		StringRedisTemplate redisTemplate
	) {
		return new SmsService(smsUtil, redisTemplate);
	}
}
