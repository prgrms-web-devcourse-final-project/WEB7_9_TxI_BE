package com.back.global.recaptcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google.captcha")
public class ReCaptchaProperties {

	private String secretKey;
	private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

	public String getVerifyUrl() {
		return VERIFY_URL;
	}
}
