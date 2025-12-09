package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.global.properties.CorsProperties;
import com.back.global.security.CustomAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomAuthenticationFilter customAuthenticationFilter;
	private final CorsProperties corsProperties;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http

			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/favicon.ico").permitAll()
				.requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 접근 허용
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 접근 허용
				.requestMatchers("/.well-known/**").permitAll()
				.anyRequest().permitAll()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**")  // H2 콘솔은 CSRF 제외
				.ignoringRequestMatchers("/swagger-ui/**") // Swagger UI는 CSRF 제외
				.ignoringRequestMatchers("/api/v1/**")  // 임시 csrf 제외
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔 iframe 허용
			);

		return http.build();

		// TODO: 나중에 인증 규칙 추가, 로그인 구현 때 추가 예정
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
