package com.back.support.helper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.api.auth.dto.cache.ActiveSessionDto;
import com.back.api.auth.service.ActiveSessionCache;
import com.back.domain.auth.entity.ActiveSession;
import com.back.domain.auth.repository.ActiveSessionRepository;
import com.back.domain.store.entity.Store;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.JwtProvider;
import com.back.global.security.SecurityUser;

@Component
public class TestAuthHelper {

	private final JwtProvider jwtProvider;
	private final ActiveSessionRepository activeSessionRepository;
	private final ActiveSessionCache activeSessionCache;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public TestAuthHelper(
		JwtProvider jwtProvider,
		ActiveSessionRepository activeSessionRepository,
		ActiveSessionCache activeSessionCache,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		this.jwtProvider = jwtProvider;
		this.activeSessionRepository = activeSessionRepository;
		this.activeSessionCache = activeSessionCache;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void authenticate(User user) {

		List<GrantedAuthority> authorities =
			List.of(new SimpleGrantedAuthority("ROLE_USER"));

		Optional<Long> storeId = Optional.ofNullable(user.getStore()).map(Store::getId);

		SecurityUser securityUser = new SecurityUser(
			user.getId(),
			user.getEmail(),
			user.getPassword(),
			user.getRole(),
			storeId,
			authorities
		);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			securityUser,
			null,
			securityUser.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/** role 지정해서 유저 생성 + accessToken 발급 */
	public String issueAccessToken(UserRole role, Store store) {
		User user = createUser(role);
		ActiveSession session = activeSessionRepository.save(ActiveSession.create(user));

		user.changeStore(store);
		userRepository.saveAndFlush(user);

		// Redis 캐시에도 저장 (테스트 환경에서 실제 인증 필터와 동일한 동작 보장)
		ActiveSessionDto dto = ActiveSessionDto.from(session);
		activeSessionCache.set(user.getId(), dto);

		return jwtProvider.generateAccessToken(user, session.getSessionId(), session.getTokenVersion());
	}

	@Transactional
	public String issueAccessToken(User user) {
		User ref = userRepository.getReferenceById(user.getId());

		ActiveSession session = activeSessionRepository.findByUserId(ref.getId())
			.orElseGet(() -> ActiveSession.create(ref));

		session.rotate();
		activeSessionRepository.saveAndFlush(session);

		// Redis 캐시에도 저장 (테스트 환경에서 실제 인증 필터와 동일한 동작 보장)
		ActiveSessionDto dto = ActiveSessionDto.from(session);
		activeSessionCache.set(ref.getId(), dto);

		return jwtProvider.generateAccessToken(ref, session.getSessionId(), session.getTokenVersion());
	}

	public User createUser(UserRole role) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		User user = User.builder()
			.email(role.name().toLowerCase() + "+" + suffix + "@test.com")
			.password(passwordEncoder.encode("pw"))
			.nickname(role.name().toLowerCase() + "_" + suffix) // 닉네임 유니크 필요하면
			.fullName("test-" + role.name())
			.role(role)
			.activeStatus(UserActiveStatus.ACTIVE)
			.birthDate(LocalDate.of(2000, 1, 1))
			.build();

		return userRepository.save(user);
	}

	public void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}
}
