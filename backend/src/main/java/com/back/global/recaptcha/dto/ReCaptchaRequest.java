package com.back.global.recaptcha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReCaptchaRequest {

	@JsonProperty("secret")
	private String secret;

	@JsonProperty("response")
	private String response;

	@JsonProperty("remoteip")
	private String remoteIp;
}
