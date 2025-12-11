package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.global.properties.CorsProperties;
import com.back.global.security.CustomAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CorsProperties corsProperties;
	private final CustomAuthenticationFilter authenticationFilter;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http

			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/favicon.ico").permitAll()
				.requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 접근 허용
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 접근 허용
				.requestMatchers("/.well-known/**").permitAll()
				.anyRequest().permitAll() // TODO: 보안 인증 설정 시 제거, 현재는 모든 API 요청을 인증없이 허용
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**")  // H2 콘솔은 CSRF 제외
				.ignoringRequestMatchers("/swagger-ui/**") // Swagger UI는 CSRF 제외
				.ignoringRequestMatchers("/api/v1/**")  // 임시 csrf 제외
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔 iframe 허용
			);

		http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(corsProperties.getAllowedOrigins());
		config.setAllowedMethods(corsProperties.getAllowedMethods());
		config.setAllowedHeaders(corsProperties.getAllowedHeaders());
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}
}
