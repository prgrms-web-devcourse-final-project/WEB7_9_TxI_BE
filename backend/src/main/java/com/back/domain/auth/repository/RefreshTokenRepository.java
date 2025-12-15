package com.back.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	long countByUserId(Long id);

	Optional<RefreshToken> findByTokenAndUserIdAndRevokedFalse(String token, Long userId);

	long countByUserIdAndRevokedFalse(Long id);

	@Modifying
	@Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
	int revokeAllByUserId(@Param("userId") Long userId);

	Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
}
