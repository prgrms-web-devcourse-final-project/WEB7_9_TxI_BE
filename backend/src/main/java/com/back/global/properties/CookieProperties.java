package com.back.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {
	private boolean secure;
	private String sameSite;
	private String domain;
}
