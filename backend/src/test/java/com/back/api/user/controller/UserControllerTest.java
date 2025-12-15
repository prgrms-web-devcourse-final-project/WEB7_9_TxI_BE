package com.back.api.user.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.UserErrorCode;
import com.back.global.http.HttpRequestContext;
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
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private TestAuthHelper testAuthHelper;

	@MockitoSpyBean
	private RefreshTokenRepository spyRefreshTokenRepository;

	@MockitoSpyBean
	private HttpRequestContext requestContext;

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

	@Nested
	@DisplayName("회원탈퇴 - `DELETE /api/v1/users/me`")
	class DeleteUserTest {

		private final String deleteUserApi = "/api/v1/users/me";

		@Test
		@DisplayName("회원탈퇴 성공 - 쿠키 삭제 + soft delete + (있다면) refreshToken 전체 revoke")
		void delete_user_success_with_revoke_all() throws Exception {
			// given
			testAuthHelper.authenticate(user);
			long userId = user.getId();

			// (선택) 현재 유저의 refresh token이 존재하는지 확인만 해두기 (없어도 테스트 진행)
			boolean hasActiveTokenBefore =
				refreshTokenRepository.findByUserIdAndRevokedFalse(userId).isPresent();

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then (API 응답)
			actions
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.name()))
				.andExpect(jsonPath("$.message").value("회원탈퇴 완료"))
				.andExpect(cookie().maxAge("accessToken", 0))
				.andExpect(cookie().maxAge("refreshToken", 0));

			// then (User soft delete 검증)
			User deletedUser = userRepository.findIncludingDeletedById(userId)
				.orElseThrow(() -> new RuntimeException("Not found user (including deleted)"));
			assertThat(deletedUser.getDeleteDate()).isNotNull();

			// then (RefreshToken revoke 검증: 토큰이 있던 경우에만 의미 있게 검증)
			if (hasActiveTokenBefore) {
				assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).isEmpty();
			}
		}

		@Test
		@DisplayName("회원탈퇴 실패 - 사용자 없음 (NOT_FOUND)")
		void delete_user_failed_by_not_found() throws Exception {
			// given
			testAuthHelper.authenticate(user);
			long userId = user.getId();

			// 유저를 물리 삭제해서 NOT_FOUND 유도
			userRepository.deleteById(userId);
			userRepository.flush();

			// when
			ResultActions actions = mvc.perform(
				delete(deleteUserApi)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			actions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
				.andExpect(jsonPath("$.message").value(UserErrorCode.NOT_FOUND.getMessage()));
		}
	}

	@Test
	@DisplayName("회원탈퇴 실패(NOT_FOUND) - revokeAll/쿠키삭제 등 부작용이 없어야 한다")
	void delete_user_failed_by_not_found_has_no_side_effects() throws Exception {
		// given
		testAuthHelper.authenticate(user);
		long userId = user.getId();

		// authenticate 과정에서 spy 호출이 발생할 수 있으니, 검증 전에 기록 제거
		clearInvocations(spyRefreshTokenRepository, requestContext);

		// NOT_FOUND 유도: 인증은 유지하되, DB에서 유저만 삭제
		userRepository.deleteById(userId);
		userRepository.flush();

		// when
		ResultActions actions = mvc.perform(
			delete("/api/v1/users/me")
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		// then: 응답 검증
		actions
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(UserErrorCode.NOT_FOUND.getMessage()));

		// then: 부작용(토큰 revoke / 쿠키 삭제) 없어야 함
		verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
		verify(requestContext, never()).deleteCookie("accessToken");
		verify(requestContext, never()).deleteCookie("refreshToken");
	}
}
