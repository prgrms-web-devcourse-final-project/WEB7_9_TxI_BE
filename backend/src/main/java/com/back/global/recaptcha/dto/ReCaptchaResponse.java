package com.back.global.recaptcha.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReCaptchaResponse {

	@JsonProperty("success")
	private boolean success;

	@JsonProperty("score")
	private Double score;

	@JsonProperty("action")
	private String action;

	@JsonProperty("challenge_ts")
	private LocalDateTime challengeTs;

	@JsonProperty("hostname")
	private String hostname;

	@JsonProperty("error-codes")
	private List<String> errorCodes;
}
