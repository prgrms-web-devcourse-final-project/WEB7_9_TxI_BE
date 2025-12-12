package com.back.api.user.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.back.global.error.code.UserErrorCode;
import com.back.support.data.TestUser;
import com.back.support.helper.TestAuthHelper;
import com.back.support.helper.UserHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private TestAuthHelper testAuthHelper;

	private User user;

	private final ObjectMapper mapper = new ObjectMapper();

	TestUser testUser;

	@BeforeEach
	void setUp() {
		testUser = userHelper.createUser(UserRole.NORMAL);
		user = testUser.user();
	}

	@Nested
	@DisplayName("GET `/api/v1/users/profile`")
	class GetUserTest {

		private final String getUserApi = "/api/v1/users/profile";

		@Test
		@DisplayName("Success get user profile info")
		void get_user_profile_success() throws Exception {
			testAuthHelper.authenticate(user);

			long userId = user.getId();

			ResultActions actions = mvc
				.perform(
					get(getUserApi)
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 조회 성공", userId)));
		}

		@Test
		@DisplayName("Failed by not allowed user")
		void failed_by_not_allowed_user() throws Exception {
			ResultActions actions = mvc
				.perform(
					get(getUserApi)
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print());

			AuthErrorCode error = AuthErrorCode.UNAUTHORIZED;

			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}

	@Nested
	@DisplayName("사용자 프로필 수정 - `PUT /api/v1/users/profile`")
	class UpdateProfileTest {
		private final String updateProfileApi = "/api/v1/users/profile";

		@Test
		@DisplayName("사용자 정보 변경 성공")
		void success_update_profile() throws Exception {
			testAuthHelper.authenticate(user);

			long userId = user.getId();

			String newNickname = "New" + user.getNickname();
			String newFullName = "New" + user.getFullName();

			String requestJson = mapper.writeValueAsString(Map.of(
				"fullName", newFullName,
				"nickname", newNickname,
				"year", "2002",
				"month", "2",
				"day", "11"
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(newNickname).isEqualTo(updatedUser.getNickname());
			assertThat(newFullName).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 성공 - 파라미터 일부만 전달")
		void success_with_missing_parameters() throws Exception {
			testAuthHelper.authenticate(user);

			long userId = user.getId();

			String newNickname = "New" + user.getNickname();

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", newNickname
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(newNickname).isEqualTo(updatedUser.getNickname());
			assertThat(user.getFullName()).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 성공 - 파라미터 없음")
		void success_with_empty_request() throws Exception {
			testAuthHelper.authenticate(user);

			long userId = user.getId();

			String requestJson = mapper.writeValueAsString(Map.of(
			));

			ResultActions actions = mvc
				.perform(
					put(updateProfileApi)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
				).andDo(print());

			actions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))
				.andExpect(jsonPath("$.message").value(String.format("%s 사용자 정보 변경 완료", userId)));

			User updatedUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user"));

			assertThat(user.getNickname()).isEqualTo(updatedUser.getNickname());
			assertThat(user.getFullName()).isEqualTo(updatedUser.getFullName());
		}

		@Test
		@DisplayName("사용자 정보 변경 실패 - 사용자 없음 (NOT_FOUND)")
		void failed_update_profile_by_user_not_found() throws Exception {
			// given
			testAuthHelper.authenticate(user);
			long userId = user.getId();

			// 인증 후 사용자 삭제
			userRepository.deleteById(userId);
			userRepository.flush(); // 즉시 반영 (중요)

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", "newName"
			));

			// when
			ResultActions actions = mvc.perform(
				put(updateProfileApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// then
			actions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
				.andExpect(jsonPath("$.message")
					.value(UserErrorCode.NOT_FOUND.getMessage()));
		}

		@Test
		@DisplayName("사용자 정보 변경 실패 - 닉네임 중복 (ALREADY_EXIST_NICKNAME)")
		void failed_update_profile_by_duplicate_nickname() throws Exception {
			// given
			testAuthHelper.authenticate(user);

			// 이미 존재하는 다른 유저 생성
			TestUser anotherUser = userHelper.createUser(UserRole.NORMAL);
			String duplicatedNickname = anotherUser.user().getNickname();

			String requestJson = mapper.writeValueAsString(Map.of(
				"nickname", duplicatedNickname
			));

			// when
			ResultActions actions = mvc.perform(
				put(updateProfileApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			// then
			actions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
				.andExpect(jsonPath("$.message")
					.value(AuthErrorCode.ALREADY_EXIST_NICKNAME.getMessage()));
		}
	}
}
