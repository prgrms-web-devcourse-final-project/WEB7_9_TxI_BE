package com.back.global.services.sms.util;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 개발/테스트 환경용 Fake SMS 발송 유틸
 * 실제 SMS를 발송하지 않고 로그만 출력하여 비용 절감
 *
 * ⚠️ HTTP 테스트 파일 자동 실행을 위한 고정 인증번호 사용
 * - 테스트/개발 환경: 항상 "123456" 사용
 * - HTTP 파일에서 "123456"으로 검증하면 자동 통과
 */
@Slf4j
@Component
@Primary
@Profile({"dev", "test", "perf"})
public class FakeSmsUtil implements SmsUtilInterface {

	@Override
	public void sendOne(String to, String verificationCode) {
		log.info("========================================");
		log.info("📱 [FAKE SMS 발송]");
		log.info("수신번호: {}", to);
		log.info("인증번호: {} (테스트 환경에서는 항상 123456 사용)", verificationCode);
		log.info("메시지: [WaitFair] 본인확인 인증번호는 {} 입니다.", verificationCode);
		log.info("💡 HTTP 테스트 시: 인증번호 123456 사용");
		log.info("========================================");
		// 실제 SMS 발송하지 않음 (비용 절감)
	}
}
