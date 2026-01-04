package com.back.global.security.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.back.global.properties.SiteProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
	private final ClientRegistrationRepository clientRegistrationRepository;

	private final SiteProperties siteProperties;

	private DefaultOAuth2AuthorizationRequestResolver defaultResolver() {
		return new DefaultOAuth2AuthorizationRequestResolver(
			clientRegistrationRepository,
			OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
		);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		OAuth2AuthorizationRequest req = defaultResolver().resolve(request);
		return customizeState(req, request);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		OAuth2AuthorizationRequest req = defaultResolver().resolve(request, clientRegistrationId);
		return customizeState(req, request);
	}

	private OAuth2AuthorizationRequest customizeState(OAuth2AuthorizationRequest authorizationRequest,
		HttpServletRequest req) {

		//OAuth 요청이 아닐 때는 넘어감
		if (authorizationRequest == null) {
			return null;
		}

		String redirectUrl = req.getParameter("redirectUrl");

		if (StringUtils.isBlank(redirectUrl)) {
			redirectUrl = siteProperties.getFrontUrl();
		}

		String originState = authorizationRequest.getState();
		if (originState == null) {
			originState = "";
		}

		String newState = originState + "#" + redirectUrl;

		// 특수문자가 포함된 경우 Base64로 인코딩
		String encodedNewState = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(newState.getBytes(StandardCharsets.UTF_8));

		return OAuth2AuthorizationRequest.from(authorizationRequest)
			.state(encodedNewState)
			.build();
	}
}
