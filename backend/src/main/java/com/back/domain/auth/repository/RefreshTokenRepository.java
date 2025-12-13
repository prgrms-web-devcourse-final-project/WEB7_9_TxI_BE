package com.back.domain.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.auth.entity.RefreshToken;
import com.back.domain.user.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

	List<RefreshToken> findAllByUserAndRevokedFalse(User user);

	long countByUserId(Long id);

	Optional<RefreshToken> findByTokenAndUserIdAndRevokedFalse(String token, Long userId);

	long countByUserIdAndRevokedFalse(Long id);
}
