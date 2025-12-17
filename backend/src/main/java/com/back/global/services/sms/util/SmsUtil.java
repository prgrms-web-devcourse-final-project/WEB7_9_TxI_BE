package com.back.global.services.sms.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 운영 환경용 실제 SMS 발송 유틸
 * 주의: 실제 Coolsms API를 호출하여 건당 비용이 발생합니다.
 */
@Slf4j
@Component
@Profile("prod")
public class SmsUtil implements SmsUtilInterface {

	@Value("${coolsms.api-Key}")
	private String apiKey;

	@Value("${coolsms.api-Secret}")
	private String apiSecretKey;

	@Value("${coolsms.sender-Number}")
	private String senderNumber;

	private DefaultMessageService messageService;

	@PostConstruct
	private void init() {
		this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecretKey, "https://api.coolsms.co.kr");
	}

	@Override
	public void sendOne(String to, String verificationCode) {
		Message message = new Message();
		message.setFrom(senderNumber);
		message.setTo(to);
		message.setText("[WaitFair] 본인확인 인증번호는 " + verificationCode + " 입니다.");

		SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
		log.info("SMS 발송 완료 - 수신번호: {}, 상태: {}", to, response.getStatusCode());
	}
}
