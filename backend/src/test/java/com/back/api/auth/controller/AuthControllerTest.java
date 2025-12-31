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
import com.back.support.helper.StoreHelper;
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
	private StoreHelper storeHelper;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", user.getEmail(),
				"password", testUser.rawPassword(),
				"fullName", user.getFullName(),
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

			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);

			String requestJson = mapper.writeValueAsString(Map.of(
				"email", existedUser.user().getEmail(),
				"password", existedUser.rawPassword(),
				"fullName", user.getFullName(),
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

		@Test
		@DisplayName("로그아웃 실패 - refreshToken 쿠키 없음 (UNAUTHORIZED)")
		void logout_failed_refresh_token_required() throws Exception {
			// given: 유저 생성
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();

			// when: refreshToken 쿠키 없이, 인증된 유저로 로그아웃 요청
			ResultActions actions = mvc.perform(
				post(logoutApi)
					.with(user(toSecurityUser(savedUser)))
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			AuthErrorCode error = AuthErrorCode.UNAUTHORIZED;

			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(error.getMessage()));

			// refresh token은 애초에 없으므로 DB에도 아무 변화 없음
			long activeTokens = tokenRepository.countByUserId(savedUser.getId());
			assertThat(activeTokens).isZero();
		}

		@Test
		@DisplayName("로그아웃 실패 - refreshToken DB에 없음 (ACCESS_OTHER_DEVICE)")
		void logout_failed_refresh_token_not_found() throws Exception {
			// given: 유저 생성 + 로그인해서 '정상 accessToken 쿠키' 확보
			TestUser existedUser = userHelper.createUser(UserRole.NORMAL, null);
			User savedUser = existedUser.user();
			String rawPassword = existedUser.rawPassword();

			String loginJson = mapper.writeValueAsString(Map.of(
				"email", savedUser.getEmail(),
				"password", rawPassword
			));

			MockHttpServletResponse loginRes = mvc.perform(
					post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson)
				)
				.andReturn()
				.getResponse();

			// 로그인 응답 쿠키에서 accessToken만 추출
			Cookie accessCookie = null;
			for (Cookie c : loginRes.getCookies()) {
				if ("accessToken".equals(c.getName())) {
					accessCookie = c;
					break;
				}
			}
			assertThat(accessCookie).isNotNull();

			// refreshToken은 "DB에 없는 값"으로 교체 (JWT 형태가 아니면 필터에서 INVALID_TOKEN로 잘릴 수 있으니 주의!)
			// => 가능하면 "형식은 JWT인데 DB/Redis에 없는 refresh"를 만들어야 함.
			// 일단 간단히는 loginRes의 refreshToken을 빼고, DB에 저장되지 않은 '진짜 JWT refresh'를 별도 발급하거나,
			// AuthTokenService/JwtProvider로 refresh를 만들어서 DB 저장 없이 넣는 방식이 가장 안정적.
			String fakeRefreshJwt
				= "eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlblR5cGUiOiJyZWZyZXNoIiwiaWQiOjEwMCwic2lkIjoiZmFrZSIsInRva2VuVmVyc2lvbiI6MSwiaWF0IjoxLCJleHAiOjk5OTk5OTk5OX0.xxx";
			Cookie refreshCookie = new Cookie("refreshToken", fakeRefreshJwt);

			// when
			ResultActions actions = mvc.perform(
					post("/api/v1/auth/logout")
						.cookie(accessCookie, refreshCookie)  // ★ access는 정상, refresh만 fake
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andDo(print());

			// then
			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthErrorCode.ACCESS_OTHER_DEVICE.getMessage()));
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
}
