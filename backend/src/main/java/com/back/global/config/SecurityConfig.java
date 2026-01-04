package com.back.global.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.ErrorCode;
import com.back.global.observability.RequestIdFilter;
import com.back.global.properties.CorsProperties;
import com.back.global.response.ApiResponse;
import com.back.global.security.CustomAuthenticationFilter;
import com.back.global.security.filter.FingerprintFilter;
import com.back.global.security.filter.IdcBlockFilter;
import com.back.global.security.filter.RateLimitFilter;
import com.back.global.security.filter.WhitelistFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용을 위해 추가
@Profile("!perf & !dev")
public class SecurityConfig {

	private final CorsProperties corsProperties;
	private final CustomAuthenticationFilter authenticationFilter;
	private final ObjectMapper objectMapper;

	// 보안 필터들 (Optional - 조건부 빈)
	private WhitelistFilter whitelistFilter;
	private IdcBlockFilter idcBlockFilter;
	private RateLimitFilter rateLimitFilter;
	private FingerprintFilter fingerprintFilter;

	public SecurityConfig(
		CorsProperties corsProperties,
		CustomAuthenticationFilter authenticationFilter,
		ObjectMapper objectMapper) {
		this.corsProperties = corsProperties;
		this.authenticationFilter = authenticationFilter;
		this.objectMapper = objectMapper;
	}

	@Autowired(required = false)
	public void setWhitelistFilter(WhitelistFilter whitelistFilter) {
		this.whitelistFilter = whitelistFilter;
	}

	@Autowired(required = false)
	public void setIdcBlockFilter(IdcBlockFilter idcBlockFilter) {
		this.idcBlockFilter = idcBlockFilter;
	}

	@Autowired(required = false)
	public void setRateLimitFilter(RateLimitFilter rateLimitFilter) {
		this.rateLimitFilter = rateLimitFilter;
	}

	@Autowired(required = false)
	public void setFingerprintFilter(FingerprintFilter fingerprintFilter) {
		this.fingerprintFilter = fingerprintFilter;
	}

	@Bean
	public RequestIdFilter requestIdFilter() {
		return new RequestIdFilter();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, RequestIdFilter requestIdFilter) throws Exception {

		AuthenticationEntryPoint entryPoint = (req, res, ex) -> {
			writeError(res, AuthErrorCode.UNAUTHORIZED);
		};

		AccessDeniedHandler deniedHandler = (req, res, ex) -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			boolean isAdmin = auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

			String uri = req.getRequestURI();

			// Normal 사용자가 Admin API 요청 시
			if (uri.startsWith("/api/v1/admin/") && !isAdmin) {
				writeError(res, AuthErrorCode.ADMIN_ONLY);
				return;
			}

			writeError(res, AuthErrorCode.FORBIDDEN);
		};

		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(deniedHandler)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/favicon.ico").permitAll()
				.requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 접근 허용
				.requestMatchers("/", "/index.html").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 접근 허용
				.requestMatchers("/.well-known/**").permitAll()
				.requestMatchers("/api/v1/auth/signup").permitAll()
				.requestMatchers("/api/v1/auth/login").permitAll()
				.requestMatchers("/api/v1/events/**").permitAll()
				.requestMatchers("/ws/**").permitAll()  // WebSocket 핸드셰이크 허용
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers("/actuator/**").permitAll()    // 모니터링/Actuator 관련
				.requestMatchers("/api/v1/tickets/entry/verify").permitAll() // QR 코드 검증
				.requestMatchers("/api/v1/**").authenticated()
				.requestMatchers("/api/v2/**").authenticated()
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**")  // H2 콘솔은 CSRF 제외
				.ignoringRequestMatchers("/swagger-ui/**") // Swagger UI는 CSRF 제외
				.ignoringRequestMatchers("/ws/**")
				.ignoringRequestMatchers("/api/v1/**")  // 임시 csrf 제외
				.ignoringRequestMatchers("/api/v2/**")  // 임시 csrf 제외
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions.sameOrigin())  // H2 콘솔 iframe 허용
			);

		// 필터 체인 등록 (개선된 방식)
		// 목표 순서: RequestId -> Whitelist -> IdcBlock -> RateLimit -> Fingerprint -> Authentication
		
		// 1. RequestId 로깅 필터 (기준점)
		http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);
		Class<? extends Filter> lastFilter = RequestIdFilter.class;

		// 2. WhitelistFilter (있으면 추가)
		if (whitelistFilter != null) {
			http.addFilterAfter(whitelistFilter, lastFilter);
			lastFilter = WhitelistFilter.class;
		}

		// 3. IdcBlockFilter (있으면 추가)
		if (idcBlockFilter != null) {
			http.addFilterAfter(idcBlockFilter, lastFilter);
			lastFilter = IdcBlockFilter.class;
		}

		// 4. RateLimitFilter (있으면 추가)
		if (rateLimitFilter != null) {
			http.addFilterAfter(rateLimitFilter, lastFilter);
			lastFilter = RateLimitFilter.class;
		}

		// 5. FingerprintFilter (있으면 추가)
		if (fingerprintFilter != null) {
			http.addFilterAfter(fingerprintFilter, lastFilter);
			lastFilter = FingerprintFilter.class;
		}

		// 6. Authentication 필터 (마지막 보안 필터 다음)
		http.addFilterAfter(authenticationFilter, lastFilter);

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
