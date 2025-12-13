package com.back.api.auth.service;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.dto.request.LoginRequest;
import com.back.api.auth.dto.request.SignupRequest;
import com.back.api.auth.dto.response.AuthResponse;
import com.back.api.auth.dto.response.TokenResponse;
import com.back.api.auth.dto.response.UserResponse;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthTokenService authTokenService;
	private final HttpRequestContext requestContext;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ErrorException(AuthErrorCode.ALREADY_EXIST_EMAIL);
		}

		if (userRepository.existsByNickname(request.nickname())) {
			throw new ErrorException(AuthErrorCode.ALREADY_EXIST_NICKNAME);
		}

		String encoded = passwordEncoder.encode(request.password());
		LocalDate birthDate = request.toBirthDate();

		User user = User.builder()
			.email(request.email())
			.password(encoded)
			.fullName(request.fullName())
			.nickname(request.nickname())
			.role(UserRole.NORMAL)
			.activeStatus(UserActiveStatus.ACTIVE)
			.birthDate(birthDate)
			.build();

		User savedUser = userRepository.save(user);

		JwtDto tokens = authTokenService.generateTokens(savedUser);

		return buildAuthResponse(savedUser, tokens);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.LOGIN_FAILED));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ErrorException(AuthErrorCode.LOGIN_FAILED);
		}

		JwtDto tokens = authTokenService.generateTokens(user);

		requestContext.setCookie("accessToken", tokens.accessToken());
		requestContext.setCookie("refreshToken", tokens.refreshToken());

		return buildAuthResponse(user, tokens);
	}

	@Transactional
	public void logout() {
		String refreshTokenStr = requestContext.getCookieValue("refreshToken", null);
		if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
			throw new ErrorException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		User currentUser = requestContext.getUser();

		RefreshToken refreshToken = refreshTokenRepository
			.findByTokenAndUserIdAndRevokedFalse(refreshTokenStr, currentUser.getId())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

		refreshToken.revoke();

		requestContext.deleteCookie("accessToken");
		requestContext.deleteCookie("refreshToken");
	}

	@Transactional
	public void verifyPassword(String rawPassword) {
		User user = requestContext.getUser();

		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new ErrorException(AuthErrorCode.PASSWORD_MISMATCH);
		}
	}

	private AuthResponse buildAuthResponse(User user, JwtDto tokens) {
		TokenResponse tokenResponse = new TokenResponse(
			tokens.tokenType(),
			tokens.accessToken(),
			tokens.accessTokenExpiresAt(),
			tokens.refreshToken(),
			tokens.refreshTokenExpiresAt()
		);

		UserResponse userResponse = new UserResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getRole()
		);

		return new AuthResponse(tokenResponse, userResponse);
	}
}
