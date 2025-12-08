package com.back.api.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.support.helper.UserHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private UserHelper userHelper;

	@Value("${custom.jwt.secret}")
	private String secret;

	private final ObjectMapper mapper = new ObjectMapper();

	@Nested
	@DisplayName("POST `/api/v1/auth/signup`")
	class SignupTest {

		private final String signUpApi = "/api/v1/auth/signup";

		@Test
		@DisplayName("Success Sign up")
		void signup_success() throws Exception {

			// TODO: Faker 사용
			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "test@test.com",
				"password", "qwer1234",
				"nickname", "테스트유저",
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

			User user = userRepository.findByEmail("test@test.com").orElseThrow(
				() -> new RuntimeException("Not found user")
			);

			assertThat(user.getNickname()).isEqualTo("테스트유저");
		}

		@Test
		@DisplayName("Failed sign up by missing params")
		void signup_failed_by_missing_params() throws Exception {

			// TODO: Faker 사용
			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "test@test.com",
				"password", "qwer1234",
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

			User user = userHelper.createUser(UserRole.NORMAL);

			// TODO: Faker 사용
			String requestJson = mapper.writeValueAsString(Map.of(
				"email", user.getEmail(),
				"password", "qwer1234",
				"nickname", "A" + user.getNickname(),
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

			User user = userHelper.createUser(UserRole.NORMAL);

			// TODO: Faker 사용
			String requestJson = mapper.writeValueAsString(Map.of(
				"email", "test" + user.getEmail(),
				"password", "qwer1234",
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

			AuthErrorCode error = AuthErrorCode.ALREADY_EXIST_NICKNAME;

			actions
				.andExpect(handler().handlerType(AuthController.class))
				.andExpect(handler().methodName("signup"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}
}
