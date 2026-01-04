package com.back.api.auth.service;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.auth.entity.ProviderType;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialAuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public User join(String providerId, String nickname, String password, ProviderType providerType) {
		if (userRepository.existsByNickname(nickname)) {
			throw new ErrorException(AuthErrorCode.ALREADY_EXIST_NICKNAME);
		}

		String raw = StringUtils.isBlank(password) ? UUID.randomUUID().toString() : password;
		String encodedPassword = passwordEncoder.encode(raw);

		User user = User.builder()
			.email(null)
			.nickname(nickname)
			.fullName(nickname)
			.password(encodedPassword)
			.providerType(providerType)
			.activeStatus(UserActiveStatus.ACTIVE)
			.role(UserRole.NORMAL)
			.providerId(providerId)
			.build();

		return userRepository.save(user);
	}

	public User modifyOrJoin(String providerId, String nickname, String password, ProviderType providerType) {
		User user = userRepository.findByProviderId(providerId).orElse(null);

		if (user == null) {
			return join(providerId, nickname, password, providerType);
		}

		user.update(nickname, nickname, null);

		return user;
	}
}
