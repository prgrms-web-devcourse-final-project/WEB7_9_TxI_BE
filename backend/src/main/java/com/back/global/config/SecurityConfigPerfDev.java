package com.back.global.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.ErrorCode;
import com.back.global.observability.RequestIdFilter;
import com.back.global.properties.CorsProperties;
import com.back.global.properties.SiteProperties;
import com.back.global.response.ApiResponse;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.oauth.CustomOAuth2AuthorizationRequestResolver;
import com.back.global.security.oauth.CustomOAuth2LoginSuccessHandler;
import com.back.global.security.oauth.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile({"perf", "dev"})
@RequiredArgsConstructor
@Slf4j
public class SecurityConfigPerfDev {

	private final CorsProperties corsProperties;
	private final SiteProperties siteProperties;
	private final CustomAuthenticationFilter authenticationFilter;
	private final ObjectMapper objectMapper;
	private final CustomOAuth2LoginSuccessHandler customOAuth2LoginSuccessHandler;
	private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;
	private final CustomOAuth2UserService customOAuth2UserService;

	@Bean
	public RequestIdFilter requestIdFilter() {
		return new RequestIdFilter();
	}

	@Bean
	SecurityFilterChain filterChain(
		HttpSecurity http,
		RequestIdFilter requestIdFilter
	) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/internal/seed/**").permitAll()
				.anyRequest().permitAll()
			)
			// ✅ perf/local에서는 CSRF를 통째로 끄는 게 보통 편함 (curl/seed 때문)
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sessionManagement ->
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2Login(oauth2 -> {
				oauth2
					.successHandler(customOAuth2LoginSuccessHandler)
					.authorizationEndpoint(authorizationEndPoint ->
						authorizationEndPoint.authorizationRequestResolver(customOAuth2AuthorizationRequestResolver))
					.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(customOAuth2UserService))
					.failureHandler((req, res, ex) -> {
						log.error("OAuth2 login failed", ex);
						writeError(res, AuthErrorCode.SOCIAL_LOGIN_FAILED);
						res.sendRedirect(siteProperties.getFrontUrl());
					});
			})
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((req, res, e) -> writeError(res, AuthErrorCode.UNAUTHORIZED))
				.accessDeniedHandler((req, res, e) -> writeError(res, AuthErrorCode.FORBIDDEN))
			);

		http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterAfter(authenticationFilter, RequestIdFilter.class);

		http
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable());

		return http.build();
	}

	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(corsProperties.getAllowedOrigins());
		config.setAllowedMethods(corsProperties.getAllowedMethods());
		config.setAllowedHeaders(corsProperties.getAllowedHeaders());
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
		response.setStatus(code.getHttpStatus().value());
		response.setContentType("application/json; charset=UTF-8");
		ApiResponse<?> body = ApiResponse.fail(code);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
