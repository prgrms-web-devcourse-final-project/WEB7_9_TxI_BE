package com.back.api.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.request.OAuthExchangeRequest;
import com.back.api.auth.store.OAuthExchangeCodeStore;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.SecurityUser;
import com.back.support.data.TestUser;
import com.back.support.factory.UserFactory;
import com.back.support.helper.UserHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository tokenRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private OAuthExchangeCodeStore codeStore;

	@Value("${custom.jwt.secret}")
	private String secret;

	private final ObjectMapper mapper = new ObjectMapper();
	private TestUser testUser;
	private User user;

	@BeforeEach
	void setUp() {
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder, null);
		user = testUser.user();
	}

	@Nested
	@DisplayName("POST `/api/v1/auth/signup`")
	class SignupTest {

		private final String signUpApi = "/api/v1/auth/signup";

		@Test
		@DisplayName("Success Sign up")
		void signup_success() throws Exception {

			Map<String, Object> body = new java.util.HashMap<>();
			body.put("email", user.getEmail());
			body.put("password", testUser.rawPassword());
			body.put("fullName", user.getFullName());
			body.put("nickname", user.getNickname());
			body.put("role", UserRole.NORMAL.name());
			body.put("year", "2002");
			body.put("month", "2");
			body.put("day", "11");
			body.put("registrationNumber", null);

			String requestJson = mapper.writeValueAsString(body);

			ResultActions actions = mvc
				.perform(
					post(signUpApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("signup"))
				.andExpect(jsonPath("$.status").value(HttpStatus.CREATED.name()))
				.andExpect(jsonPath("$.message").value("회원가입 성공"));

			User savedUser = userRepository.findByEmail(user.getEmail()).orElseThrow(
				() -> new RuntimeException("Not found user")
			);

			assertThat(savedUser.getNickname()).isEqualTo(user.getNickname());
		}

		@Test
		@DisplayName("Failed sign up by missing params")
		void signup_failed_by_missing_params() throws Exception {

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", user.getEmail(),
				"password", testUser.rawPassword(),
				"role", UserRole.NORMAL.name(),
				"year", "2002",
				"month", "2",
				"day", "11"
			));

			ResultActions actions = mvc
				.perform(
					post(signUpApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("signup"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("Failed sign up by existing email")
		void signup_failed_by_existing_email() throws Exception {

			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", existedUser.user().getEmail(),
				"password", existedUser.rawPassword(),
				"fullName", user.getFullName(),
				"role", UserRole.NORMAL.name(),
				"nickname", "A" + existedUser.user().getNickname(),
				"year", "2002",
				"month", "2",
				"day", "11"
			));

			ResultActions actions = mvc
				.perform(
					post(signUpApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			AuthErrorCode error = AuthErrorCode.ALREADY_EXIST_EMAIL;

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("signup"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}

		@Test
		@DisplayName("Failed sign up by existing nickname")
		void signup_failed_by_existing_nickname() throws Exception {

			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "test" + existedUser.user().getEmail(),
				"password", existedUser.rawPassword(),
				"fullName", user.getFullName(),
				"nickname", existedUser.user().getNickname(),
				"role", UserRole.NORMAL.name(),
				"year", "2002",
				"month", "2",
				"day", "11"
			));

			ResultActions actions = mvc
				.perform(
					post(signUpApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			AuthErrorCode error = AuthErrorCode.ALREADY_EXIST_NICKNAME;

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("signup"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}

	@Nested
	@DisplayName("POST `/api/v1/auth/login`")
	class LoginTest {

		private final String loginApi = "/api/v1/auth/login";

		@Test
		@DisplayName("Success login")
		void login_success() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions actions = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(jsonPath("$.status").value(HttpStatus.CREATED.name()))
				.andExpect(jsonPath("$.message").value("로그인 성공"));
		}

		@Test
		@DisplayName("Failed by missing password parameter")
		void failed_by_missing_password_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail()
			));

			ResultActions actions = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요."));
		}

		@Test
		@DisplayName("Failed by missing email parameter")
		void failed_by_missing_email_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"password", rawPassword
			));

			ResultActions actions = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이메일을 입력해주세요."));
		}

		@Test
		@DisplayName("Failed by wrong email parameter")
		void failed_by_wrong_email_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "A" + savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions actions = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			AuthErrorCode error = AuthErrorCode.LOGIN_FAILED;

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}

		@Test
		@DisplayName("Failed by wrong password parameter")
		void failed_by_wrong_password_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", "A" + rawPassword
			));

			ResultActions actions = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			AuthErrorCode error = AuthErrorCode.LOGIN_FAILED;

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}

		@Test
		@DisplayName("싱글디바이스 정책: 두 번째 로그인 후 첫 번째 accessToken으로 보호 API 호출하면 ACCESS_OTHER_DEVICE")
		void old_access_blocked_after_second_login() throws Exception {
			// given
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			// 1st login
			ResultActions login1 = mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson));

			String body1 = login1.andReturn().getResponse().getContentAsString();
			var node1 = mapper.readTree(body1);
			String access1 = node1.path("data").path("tokens").path("accessToken").asText();

			// 2nd login (rotate + revoke + redis overwrite)
			mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson));

			// when: old access로 보호 API 호출
			ResultActions blocked = mvc.perform(
				get("/api/v1/some-resource")
					.header("Authorization", "Bearer " + access1)
			).andDo(print());

			// then: 401 + ACCESS_OTHER_DEVICE 메시지
			blocked
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthErrorCode.ACCESS_OTHER_DEVICE.getMessage()));
		}
	}

	@Nested
	@DisplayName("로그아웃 API Test `POST /api/v1/auth/logout`")
	class LogoutTest {
		private final String logoutApi = "/api/v1/auth/logout";

		private SecurityUser toSecurityUser(User user) {
			return new SecurityUser(
				user.getId(),
				user.getPassword(),
				user.getNickname(),
				user.getRole(),
				null,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
		}

		@Test
		@DisplayName("로그아웃 성공")
		void logout_success() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String loginJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions loginActions = mvc.perform(
				post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson)
			).andDo(print());

			// 로그인 응답에서 쿠키 꺼내기. (accessToken, refreshToken 둘 다 있을 것)
			MockHttpServletResponse loginResponse = loginActions.andReturn().getResponse();
			Cookie[] cookies = loginResponse.getCookies();
			assertThat(cookies).isNotEmpty();

			SecurityUser securityUser = toSecurityUser(savedUser);

			ResultActions actions = mvc.perform(
				post(logoutApi)
					.with(user(securityUser))
					.cookie(cookies)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			actions.andExpect(status().isNoContent());

			long activeTokens = tokenRepository.countByUserIdAndRevokedFalse(savedUser.getId());
			assertThat(activeTokens).isZero();
		}
	}

	@Nested
	@DisplayName("비밀번호 일치 확인 API - `POST /api/v1/auth/verify-password`")
	class VerifyPasswordTest {

		private final String verifyPasswordApi = "/api/v1/auth/verify-password";

		private SecurityUser toSecurityUser(User user) {
			return new SecurityUser(
				user.getId(),
				user.getPassword(),
				user.getNickname(),
				user.getRole(),
				null,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
		}

		@Test
		@DisplayName("비밀번호 인증 성공")
		void verify_password_success() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String loginJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions loginActions = mvc.perform(
				post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson)
			).andDo(print());

			MockHttpServletResponse loginResponse = loginActions.andReturn().getResponse();
			Cookie[] cookies = loginResponse.getCookies();
			assertThat(cookies).isNotEmpty();

			SecurityUser securityUser = toSecurityUser(savedUser);

			String requestJson = mapper.writeValueAsString(Map.of(
				"password", rawPassword
			));

			ResultActions actions = mvc.perform(
				post(verifyPasswordApi)
					.with(user(securityUser))
					.cookie(cookies)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			actions.andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("비밀번호 인증 실패 - 요청 데이터 누락")
		void failed_by_missing_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String loginJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions loginActions = mvc.perform(
				post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson)
			).andDo(print());

			MockHttpServletResponse loginResponse = loginActions.andReturn().getResponse();
			Cookie[] cookies = loginResponse.getCookies();
			assertThat(cookies).isNotEmpty();

			SecurityUser securityUser = toSecurityUser(savedUser);

			ResultActions actions = mvc.perform(
				post(verifyPasswordApi)
					.with(user(securityUser))
					.cookie(cookies)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			actions.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("비밀번호 인증 실패 - 비밀번호 불일치")
		void failed_by_wrong_parameter() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String loginJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			ResultActions loginActions = mvc.perform(
				post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson)
			).andDo(print());

			MockHttpServletResponse loginResponse = loginActions.andReturn().getResponse();
			Cookie[] cookies = loginResponse.getCookies();
			assertThat(cookies).isNotEmpty();

			SecurityUser securityUser = toSecurityUser(savedUser);

			String requestJson = mapper.writeValueAsString(Map.of(
				"password", "A" + rawPassword
			));

			ResultActions actions = mvc.perform(
				post(verifyPasswordApi)
					.with(user(securityUser))
					.cookie(cookies)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			AuthErrorCode error = AuthErrorCode.PASSWORD_MISMATCH;

			actions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}

	@Nested
	@DisplayName("소셜 로그인 토큰 반환 API")
	class TokenExchangeTest {
		@Test
		@DisplayName("exchange 성공: 200 OK + AuthResponse 반환")
		void exchange_success() throws Exception {
			// given: 유저를 DB에 만들어 둔다
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			Long userId = existedUser.user().getId();

			// codeStore가 code를 소비하면 userId를 반환하도록
			when(codeStore.consume("valid_code")).thenReturn(Optional.of(userId));

			OAuthExchangeRequest request = new OAuthExchangeRequest("valid_code");

			// when & then
			mvc.perform(post("/api/v1/auth/oauth/exchange")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.userId").value(userId))
				.andExpect(jsonPath("$.data.user.email").value(existedUser.user().getEmail()))
				.andExpect(jsonPath("$.data.tokens.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokens.accessTokenExpiresAt").isNumber())
				.andExpect(jsonPath("$.data.tokens.refreshTokenExpiresAt").isNumber());
		}

		@Test
		@DisplayName("exchange 실패: code 소비 실패(SOCIAL_LOGIN_FAILED) -> 400")
		void exchange_fail_socialLoginFailed() throws Exception {
			// given
			when(codeStore.consume("invalid_or_expired_code")).thenReturn(Optional.empty());

			OAuthExchangeRequest request = new OAuthExchangeRequest("invalid_or_expired_code");

			mvc.perform(post("/api/v1/auth/oauth/exchange")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value(AuthErrorCode.SOCIAL_LOGIN_FAILED.getMessage()));
		}

		@Test
		@DisplayName("exchange 실패: validation - code가 blank면 400")
		void exchange_fail_validation_blankCode() throws Exception {
			// given
			OAuthExchangeRequest request = new OAuthExchangeRequest(" ");

			mvc.perform(post("/api/v1/auth/oauth/exchange")
					.contentType("application/json")
					.content(mapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}
	}
}
