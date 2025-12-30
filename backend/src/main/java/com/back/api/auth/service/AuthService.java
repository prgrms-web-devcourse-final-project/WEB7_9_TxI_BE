package com.back.api.auth.service;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.JwtDto;
import com.back.api.auth.dto.cache.ActiveSessionDto;
import com.back.api.auth.dto.request.LoginRequest;
import com.back.api.auth.dto.request.SignupRequest;
import com.back.api.auth.dto.response.AuthResponse;
import com.back.api.auth.dto.response.TokenResponse;
import com.back.api.auth.dto.response.UserResponse;
import com.back.api.auth.store.AuthStore;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.auth.repository.RefreshTokenRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.error.code.AuthErrorCode;
import com.back.global.error.exception.ErrorException;
import com.back.global.http.HttpRequestContext;
import com.back.global.utils.TokenHash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthTokenService authTokenService;
	private final HttpRequestContext requestContext;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ActiveSessionCache activeSessionCache;
	private final ActiveSessionRepository activeSessionRepository;
	private final AuthStore authStore;

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

		JwtDto tokens = loginAsSingleDevice(savedUser);

		return buildAuthResponse(savedUser, tokens);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmailAndDeleteDateIsNull(request.email())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.LOGIN_FAILED));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ErrorException(AuthErrorCode.LOGIN_FAILED);
		}

		JwtDto tokens = loginAsSingleDevice(user);

		return buildAuthResponse(user, tokens);
	}

	@Transactional
	public void logout() {
		String refreshTokenStr = requestContext.getCookieValue("refreshToken", null);
		if (StringUtils.isBlank(refreshTokenStr)) {
			throw new ErrorException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		long userId = requestContext.getUserId();

		String refreshHash = TokenHash.sha256(refreshTokenStr);

		RefreshToken refreshToken = refreshTokenRepository
			.findByTokenAndUserIdAndRevokedFalse(refreshHash, requestContext.getUserId())
			.orElseThrow(() -> new ErrorException(AuthErrorCode.ACCESS_OTHER_DEVICE));

		refreshToken.revoke();
		authStore.deleteRefreshCache(userId);

		// ActiveSession 캐시 무효화
		activeSessionCache.evict(userId);

		requestContext.deleteAuthCookies();
	}

	@Transactional
	public void verifyPassword(String rawPassword) {
		User user = requestContext.getUser();

		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new ErrorException(AuthErrorCode.PASSWORD_MISMATCH);
		}
	}

	private JwtDto loginAsSingleDevice(User user) {
		ActiveSession session = getOrCreateActiveSession(user);

		session.rotate();

		ActiveSession saved = activeSessionRepository.saveAndFlush(session);

		refreshTokenRepository.revokeAllActiveByUserId(user.getId());
		authStore.deleteRefreshCache(user.getId());

		// 로그인 직후 Redis에 캐싱 불필요 DB접근 최소화
		ActiveSessionDto dto = ActiveSessionDto.from(saved);
		activeSessionCache.set(user.getId(), dto);

		JwtDto tokens = authTokenService.issueTokens(user, saved.getSessionId(), saved.getTokenVersion());

		requestContext.setAccessTokenCookie(tokens.accessToken());
		requestContext.setRefreshTokenCookie(tokens.refreshToken());

		return tokens;
	}

	/**
	 * ActiveSession 조회 또는 생성 (동시 로그인 race condition 처리)
	 * - findByUserIdForUpdate로 비관적 락 획득
	 * - 최초 로그인 시 중복 생성 시도로 인한 UniqueConstraint 위반 방지
	 */
	private ActiveSession getOrCreateActiveSession(User user) {
		// 비관적 락으로 기존 세션 조회
		var existing = activeSessionRepository.findByUserIdForUpdate(user.getId());
		if (existing.isPresent()) {
			return existing.get();
		}

		// 최초 로그인 시 세션 생성
		try {
			return activeSessionRepository.save(ActiveSession.create(user));
		} catch (Exception e) {
			// [동시성 처리] UniqueConstraint 위반 시 재조회
			// - 동시에 2개 로그인 요청이 오면:
			//   Thread A: findByUserIdForUpdate → empty → save
			//   Thread B: findByUserIdForUpdate → empty → save (실패)
			// - Thread B는 여기서 catch하고 Thread A가 생성한 세션 재조회
			return activeSessionRepository.findByUserIdForUpdate(user.getId())
				.orElseThrow(() -> new ErrorException(AuthErrorCode.UNAUTHORIZED));
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
