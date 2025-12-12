package com.back.api.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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

	@Value("${custom.jwt.secret}")
	private String secret;

	private final ObjectMapper mapper = new ObjectMapper();
	private TestUser testUser;
	private User user;

	@BeforeEach
	void setUp() {
		testUser = UserFactory.fakeUser(UserRole.NORMAL, passwordEncoder);
		user = testUser.user();
	}

	@Nested
	@DisplayName("POST `/api/v1/auth/signup`")
	class SignupTest {

		private final String signUpApi = "/api/v1/auth/signup";

		@Test
		@DisplayName("Success Sign up")
		void signup_success() throws Exception {

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", user.getEmail(),
				"password", testUser.rawPassword(),
				"nickname", user.getNickname(),
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
				"password", user.getPassword(),
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

			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", existedUser.user().getEmail(),
				"password", existedUser.rawPassword(),
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

			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "test" + existedUser.user().getEmail(),
				"password", existedUser.rawPassword(),
				"nickname", existedUser.user().getNickname(),
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
		@DisplayName("한 사용자가 여러 기기에서 동시에 로그인할 수 있다")
		void login_multi_devices_success() throws Exception {
			// given: 하나의 유저 생성
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			// when: 같은 계정으로 1번째 기기 로그인
			ResultActions actions1 = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// and: 같은 계정으로 2번째 기기 로그인
			ResultActions actions2 = mvc.perform(
				post(loginApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// then: 두 요청 모두 성공 응답
			actions1
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(jsonPath("$.status").value(HttpStatus.CREATED.name()))
				.andExpect(jsonPath("$.message").value("로그인 성공"));

			actions2
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("login"))
				.andExpect(jsonPath("$.status").value(HttpStatus.CREATED.name()))
				.andExpect(jsonPath("$.message").value("로그인 성공"));

			// 응답 바디 파싱
			String body1 = actions1.andReturn().getResponse().getContentAsString();
			String body2 = actions2.andReturn().getResponse().getContentAsString();

			var node1 = mapper.readTree(body1);
			var node2 = mapper.readTree(body2);

			long userId1 = node1.path("data").path("user").path("userId").asLong();
			long userId2 = node2.path("data").path("user").path("userId").asLong();

			String accessToken1 = node1.path("data").path("tokens").path("accessToken").asText();
			String accessToken2 = node2.path("data").path("tokens").path("accessToken").asText();

			String refreshToken1 = node1.path("data").path("tokens").path("refreshToken").asText();
			String refreshToken2 = node2.path("data").path("tokens").path("refreshToken").asText();

			// 같은 유저로부터 발급된 토큰인지 확인
			assertThat(userId1).isEqualTo(savedUser.getId());
			assertThat(userId2).isEqualTo(savedUser.getId());

			// 각 요청마다 토큰이 정상 발급되었는지
			assertThat(accessToken1).isNotBlank();
			assertThat(accessToken2).isNotBlank();
			assertThat(refreshToken1).isNotBlank();
			assertThat(refreshToken2).isNotBlank();

			// DB에서 refreshToken 이 2개인지 검증
			long tokenCount = tokenRepository.countByUserId(savedUser.getId());
			assertThat(tokenCount).isEqualTo(2);

			assertThat(refreshToken1).isNotEqualTo(refreshToken2);
			assertThat(accessToken1).isNotEqualTo(accessToken2);
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
				java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
		}

		@Test
		@DisplayName("로그아웃 성공")
		void logout_success() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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

		@Test
		@DisplayName("로그아웃 실패 - refreshToken 쿠키 없음 (REFRESH_TOKEN_REQUIRED)")
		void logout_failed_refresh_token_required() throws Exception {
			// given: 유저 생성
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
			User savedUser = existedUser.user();

			// when: refreshToken 쿠키 없이, 인증된 유저로 로그아웃 요청
			ResultActions actions = mvc.perform(
				post(logoutApi)
					.with(user(toSecurityUser(savedUser)))
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			AuthErrorCode error = AuthErrorCode.REFRESH_TOKEN_REQUIRED;

			actions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));

			// refresh token은 애초에 없으므로 DB에도 아무 변화 없음
			long activeTokens = tokenRepository.countByUserId(savedUser.getId());
			assertThat(activeTokens).isZero();
		}

		@Test
		@DisplayName("로그아웃 실패 - refreshToken DB에 없음 (REFRESH_TOKEN_NOT_FOUND)")
		void logout_failed_refresh_token_not_found() throws Exception {
			// given: 유저 하나 생성 (하지만 이 유저에 대한 refreshToken 레코드는 없음)
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
			User savedUser = existedUser.user();

			// 가짜 refreshToken 값 (DB에는 저장하지 않음)
			String fakeRefreshToken = "this-refresh-token-does-not-exist";
			Cookie refreshCookie = new Cookie("refreshToken", fakeRefreshToken);

			// when: 가짜 refreshToken 쿠키 + 인증된 유저로 로그아웃 요청
			ResultActions actions = mvc.perform(
				post(logoutApi)
					.with(user(toSecurityUser(savedUser))) // SecurityContext에 유저 세팅
					.cookie(refreshCookie)                 // 하지만 DB엔 없는 토큰
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			AuthErrorCode error = AuthErrorCode.REFRESH_TOKEN_NOT_FOUND;

			actions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(error.getMessage()));

			// 여전히 이 유저의 refreshToken은 DB에 없음
			long activeTokens = tokenRepository.countByUserId(savedUser.getId());
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
				java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
		}

		@Test
		@DisplayName("비밀번호 인증 성공")
		void verify_password_success() throws Exception {
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL);
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
}
