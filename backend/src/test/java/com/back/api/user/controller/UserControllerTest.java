package com.back.api.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.security.SecurityUser;
import com.back.support.data.TestUser;
import com.back.support.helper.UserHelper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserHelper userHelper;
	private User user;
	SecurityUser securityUser;
	UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	void setUp() {
		TestUser testUser = userHelper.createUser(UserRole.NORMAL);
		user = testUser.user();
	}

	@Nested
	@DisplayName("GET `/api/v1/users/profile")
	class GetUserTest {

		private final String getUserApi = "/api/v1/users/profile";

		@Test
		@DisplayName("Success get user profile info")
		void get_user_profile_success() throws Exception {
			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(
				user.getId(),
				user.getPassword(),
				user.getNickname(),
				UserRole.NORMAL, authorities
			);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			long userId = user.getId();

			ResultActions actions = mvc
				.perform(
					get(getUserApi, userId)
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
			long userId = user.getId();

			ResultActions actions = mvc
				.perform(
					get(getUserApi, userId)
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print());

			AuthErrorCode error = AuthErrorCode.UNAUTHORIZED;

			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}

		@Test
		@DisplayName("Failed by wrong user id")
		void failed_by_wrong_user_id() throws Exception {
			long userId = user.getId();

			ResultActions actions = mvc
				.perform(
					get(getUserApi, userId)
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print());

			AuthErrorCode error = AuthErrorCode.UNAUTHORIZED;

			actions
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.name()))
				.andExpect(jsonPath("$.message").value(error.getMessage()));
		}
	}
}
