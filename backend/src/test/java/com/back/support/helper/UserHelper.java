package com.back.support.helper;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.support.data.TestUser;
import com.back.support.factory.UserFactory;

@Component
public class UserHelper {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	UserHelper(UserRepository userRepository, PasswordEncoder encoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = encoder;
	}

	public TestUser createUser(UserRole role) {
		TestUser testUser = UserFactory.fakeUser(role, this.passwordEncoder);
		saveUser(testUser.user());
		return testUser;
	}

	public User saveUser(User user) {
		return userRepository.save(user);
	}
}
