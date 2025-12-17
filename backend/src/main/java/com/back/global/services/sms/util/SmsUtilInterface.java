package com.back.global.services.sms.util;

/**
 * SMS 발송 인터페이스
 */
public interface SmsUtilInterface {

	/**
	 * 단일 SMS 메시지 발송
	 * @param to 수신번호 (하이픈 제거된 형태: 01012345678)
	 * @param verificationCode 인증번호
	 */
	void sendOne(String to, String verificationCode);
}
