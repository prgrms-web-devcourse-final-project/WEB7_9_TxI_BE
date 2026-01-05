package com.back.global.security.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.back.api.auth.store.OAuthExchangeCodeStore;
import com.back.global.http.HttpRequestContext;
import com.back.global.properties.SiteProperties;
import com.back.global.security.SecurityUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final HttpRequestContext httpRequestContext;
	private final SiteProperties siteProperties;
	private final OAuthExchangeCodeStore codeStore;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException {
		SecurityUser securityUser = (SecurityUser)authentication.getPrincipal();
		Long userId = securityUser.getId();

		String state = request.getParameter("state");
		String redirectUrl = siteProperties.getFrontUrl() + "/oauth/callback";

		if (state != null && !state.isBlank()) {
			String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
			int idx = decoded.indexOf('#');
			log.info("Provider OAuth2 decoded: {}", decoded);
			if (idx >= 0 && idx + 1 < decoded.length()) {
				redirectUrl = decoded.substring(idx + 1);
			}
		}

		String code = codeStore.issue(userId);

		String encoded = URLEncoder.encode(code, StandardCharsets.UTF_8);
		String separator = redirectUrl.contains("?") ? "&" : "?";
		String finalRedirect = redirectUrl + separator + "code=" + encoded;

		log.info("Provider OAuth2 Redirect Url: {}", finalRedirect);

		httpRequestContext.sendRedirect(finalRedirect);
	}
}
