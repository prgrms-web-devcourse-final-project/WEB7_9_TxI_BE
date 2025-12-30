package com.back.global.http;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HttpRequestMetaProvider implements RequestMetaProvider {
	private final HttpRequestContext requestContext;

	@Override
	public String userAgent() {
		return requestContext.getUserAgent();
	}

	@Override
	public String clientIp() {
		return requestContext.getClientIp();
	}
}
