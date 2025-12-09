package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.factory.UserFactory;

@Component
public class UserHelper {

	private final UserRepository userRepository;

	UserHelper(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User createUser(UserRole role) {
		return userRepository.save(UserFactory.fakeUser(role));
	}
}
