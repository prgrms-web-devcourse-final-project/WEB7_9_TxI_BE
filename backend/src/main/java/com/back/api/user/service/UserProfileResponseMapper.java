package com.back.api.user.service;

import java.time.format.DateTimeFormatter;

import com.back.api.user.dto.response.UserProfileResponse;
import com.back.domain.user.entity.User;

public class UserProfileResponseMapper {

	private static final DateTimeFormatter DATE_FORMATTER
		= DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static UserProfileResponse from(User user) {
		String birthDateStr = user.getBirthDate() != null
			? user.getBirthDate().format(DATE_FORMATTER)
			: null;

		String signupDateStr = user.getCreateAt() != null
			? user.getCreateAt().toLocalDate().format(DATE_FORMATTER)
			: null;

		return new UserProfileResponse(
			user.getId(),
			user.getEmail(),
			user.getFullName(),
			user.getNickname(),
			user.getRole(),
			birthDateStr,
			signupDateStr
		);
	}
}
