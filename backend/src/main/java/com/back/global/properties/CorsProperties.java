package com.back.global.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "custom.cors")
public class CorsProperties {
	private List<String> allowedOrigins;
	private List<String> allowedMethods;
	private List<String> allowedHeaders;
}
