package com.back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.global.logging.RequestIdFilter;
import com.back.global.properties.CorsProperties;
import com.back.global.security.CustomAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용을 위해 추가
@RequiredArgsConstructor
public class SecurityConfig {

	private final CorsProperties corsProperties;
	private final CustomAuthenticationFilter authenticationFilter;
	private final ObjectMapper objectMapper;

	@Bean
	public RequestIdFilter requestIdFilter() {
		return new RequestIdFilter();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, RequestIdFilter requestIdFilter) throws Exception {
		http

			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/favicon.ico").permitAll()
				.requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 접근 허용
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 접근 허용
				.requestMatchers("/.well-known/**").permitAll()
				.requestMatchers("/api/v1/auth/signup").permitAll()
				.requestMatchers("/api/v1/auth/login").permitAll()
				.requestMatchers("/api/v1/admin/auth/**").permitAll()
				.requestMatchers("/ws/**").permitAll()  // WebSocket 핸드셰이크 허용
				// .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // TODO: 프론트 개발 완료 후 권한 활성화
				.requestMatchers("/actuator/**").permitAll()    // 모니터링/Actuator 관련
				.requestMatchers("/api/v1/**").authenticated()
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**")  // H2 콘솔은 CSRF 제외
				.ignoringRequestMatchers("/swagger-ui/**") // Swagger UI는 CSRF 제외
				.ignoringRequestMatchers("/ws/**")
				.ignoringRequestMatchers("/api/v1/**")  // 임시 csrf 제외
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔 iframe 허용
			);

		// MDC RequestId로깅용 필터 클래스 순서보장
		http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);
		// MDC에서 requestId식별 후에 authenticationFilter적용
		http.addFilterAfter(authenticationFilter, RequestIdFilter.class);

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

	@Bean
	public PasswordEncoder passwordEncoder(
		@Value("${security.password.bcrypt-strength}") int strength
	) {
		return new BCryptPasswordEncoder(strength);
	}
}
