package com.back.global.services.sms.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.global.services.sms.util.SmsUtilInterface;

import lombok.extern.slf4j.Slf4j;

/**
 * 테스트/개발 환경용 SMS 서비스
 * 고정 인증번호를 사용하여 HTTP 테스트 자동화 지원
 */
@Slf4j
public class TestSmsService extends SmsService {

	private static final long VERIFICATION_CODE_TTL = 180L; // 3분
	private static final String REDIS_KEY_PREFIX = "SMS_VERIFY:";

	private final String fixedCode;
	private final SmsUtilInterface smsUtil;
	private final StringRedisTemplate redisTemplate;

	public TestSmsService(
		SmsUtilInterface smsUtil,
		StringRedisTemplate redisTemplate,
		String fixedCode
	) {
		super(smsUtil, redisTemplate);
		this.smsUtil = smsUtil;
		this.redisTemplate = redisTemplate;
		this.fixedCode = fixedCode;
	}

	/**
	 * 고정 인증번호 발송 (테스트용)
	 */
	@Override
	public void sendVerificationCode(String phoneNum) {
		// 고정 인증번호 사용
		String verificationCodeStr = fixedCode;

		// SMS 발송 (FakeSmsUtil이 로그 출력)
		smsUtil.sendOne(phoneNum, verificationCodeStr);

		// Redis에 인증번호 저장 (TTL: 3분)
		String redisKey = REDIS_KEY_PREFIX + phoneNum;
		redisTemplate.opsForValue().set(
			redisKey,
			verificationCodeStr,
			Duration.ofSeconds(VERIFICATION_CODE_TTL)
		);

		log.info("테스트용 고정 인증번호 발송 완료 - 전화번호: {}, 인증번호: {}", phoneNum, fixedCode);
	}
}
