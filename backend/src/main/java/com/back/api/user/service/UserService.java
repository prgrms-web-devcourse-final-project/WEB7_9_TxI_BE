package com.back.api.user.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.user.dto.request.UpdateProfileRequest;
import com.back.api.user.dto.response.UserProfileResponse;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.code.UserErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final HttpRequestContext requestContext;

	public UserProfileResponse getUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		return UserProfileResponseMapper.from(user);
	}

	@Transactional
	public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		String newNickname = request.nickname() != null ? request.nickname() : user.getNickname();
		String newFullName = request.fullName() != null ? request.fullName() : user.getFullName();
		LocalDate newBirthDate = request.toBirthDate() != null ? request.toBirthDate() : user.getBirthDate();

		if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
			if (userRepository.existsByNickname(request.nickname())) {
				throw new ErrorException(AuthErrorCode.ALREADY_EXIST_NICKNAME);
			}
		}

		user.update(newFullName, newNickname, newBirthDate);

		return UserProfileResponseMapper.from(user);
	}

	@Transactional
	public void deleteUser(long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ErrorException(UserErrorCode.NOT_FOUND));

		// 모든 기기의 refreshToken 무효화 (전 기기 로그아웃 효과)
		refreshTokenRepository.revokeAllByUserId(userId);

		// 현재 요청의 쿠키 삭제 (현재 기기 즉시 로그아웃 UX)
		requestContext.deleteAuthCookies();

		user.softDelete();
	}
}
