package com.back.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

	private Test test = new Test();

	@Getter
	@Setter
	public static class Test {
		private boolean enabled = false;
		private String fixedCode = "123456";
	}
}
