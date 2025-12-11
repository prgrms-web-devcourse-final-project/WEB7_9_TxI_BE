package com.back.api.user.service;

import org.springframework.stereotype.Service;

import com.back.api.user.dto.response.UserProfileResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.UserErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	public UserProfileResponse getUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		return UserProfileResponseMapper.from(user);
	}
}
