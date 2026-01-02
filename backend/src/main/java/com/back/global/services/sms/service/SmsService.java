package com.back.global.services.sms.service;

import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.global.error.code.SmsErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.services.sms.util.SmsUtilInterface;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SmsService {

	private final SmsUtilInterface smsUtil;
	private final StringRedisTemplate redisTemplate;

	private static final long VERIFICATION_CODE_TTL = 180L; // 3분
	private static final long VERIFIED_FLAG_TTL = 600L; // 10분 - 인증 완료 후 사전등록까지 유효 시간
	private static final String REDIS_KEY_PREFIX = "SMS_VERIFY:";
	private static final String SMS_VERIFIED_PREFIX = "SMS_VERIFIED:";

	/**
	 * 전화번호 마스킹 처리 (중간 4자리를 ****로 변환)
	 * 예: 01012345678 -> 010****5678
	 * @param phoneNum 원본 전화번호
	 * @return 마스킹된 전화번호
	 */
	private String maskPhoneNumber(String phoneNum) {
		if (phoneNum == null || phoneNum.length() < 8) {
			return phoneNum;
		}

		// 01012345678 -> 010****5678 형태로 변환
		int length = phoneNum.length();
		String prefix = phoneNum.substring(0, 3);  // 010
		String suffix = phoneNum.substring(length - 4);  // 5678
		return prefix + "****" + suffix;
	}

	/**
	 * 인증번호 발송
	 * @param phoneNum 수신 전화번호 (하이픈 제거된 형태)
	 * @return 인증번호 유효 시간(초)
	 */
	public Long sendVerificationCode(String phoneNum) {
		// 6자리 랜덤 인증번호 생성
		Random random = new Random();
		int verificationCode = random.nextInt(900000) + 100000;
		String verificationCodeStr = String.valueOf(verificationCode);

		// SMS 발송
		try {
			smsUtil.sendOne(phoneNum, verificationCodeStr);
		} catch (Exception e) {
			log.error("SMS 발송 실패 - 전화번호: {}, 오류: {}", maskPhoneNumber(phoneNum), e.getMessage());
			throw new ErrorException(SmsErrorCode.SMS_SEND_FAILED);
		}

		// Redis에 인증번호 저장 (TTL: 3분)
		try {
			String redisKey = REDIS_KEY_PREFIX + phoneNum;
			redisTemplate.opsForValue().set(redisKey, verificationCodeStr, Duration.ofSeconds(VERIFICATION_CODE_TTL));
			log.info("인증번호 발송 및 Redis 저장 완료 - 전화번호: {}", maskPhoneNumber(phoneNum));
		} catch (Exception e) {
			log.error("Redis 저장 실패 - 전화번호: {}, 오류: {}", maskPhoneNumber(phoneNum), e.getMessage());
			throw new ErrorException(SmsErrorCode.SMS_SEND_FAILED);
		}

		return VERIFICATION_CODE_TTL;
	}

	/**
	 * 인증번호 검증
	 * @param phoneNum 전화번호
	 * @param verificationCode 사용자가 입력한 인증번호
	 * @return 인증 성공 여부
	 */
	public boolean verifyCode(String phoneNum, String verificationCode) {
		String redisKey = REDIS_KEY_PREFIX + phoneNum;
		String storedCode = redisTemplate.opsForValue().get(redisKey);

		if (storedCode == null) {
			log.warn("인증번호 만료 또는 존재하지 않음 - 전화번호: {}", maskPhoneNumber(phoneNum));
			throw new ErrorException(SmsErrorCode.VERIFICATION_CODE_NOT_FOUND);
		}

		boolean isValid = storedCode.equals(verificationCode);

		if (isValid) {
			// 인증 성공 시 인증 코드 삭제 (재사용 방지)
			redisTemplate.delete(redisKey);

			// 인증 완료 플래그 저장 (사전등록 시 검증용, TTL: 10분)
			String verifiedKey = SMS_VERIFIED_PREFIX + phoneNum;
			redisTemplate.opsForValue().set(verifiedKey, "true", Duration.ofSeconds(VERIFIED_FLAG_TTL));

			log.info("SMS 인증 성공 및 완료 플래그 저장 - 전화번호: {}", maskPhoneNumber(phoneNum));
		} else {
			log.warn("SMS 인증 실패 - 전화번호: {}, 입력값: {}", maskPhoneNumber(phoneNum), verificationCode);
			throw new ErrorException(SmsErrorCode.VERIFICATION_CODE_MISMATCH);
		}

		return isValid;
	}

	/**
	 * 인증번호가 존재하는지 확인
	 * @param phoneNum 전화번호
	 * @return 존재 여부
	 */
	public boolean hasVerificationCode(String phoneNum) {
		String redisKey = REDIS_KEY_PREFIX + phoneNum;
		return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
	}
}
