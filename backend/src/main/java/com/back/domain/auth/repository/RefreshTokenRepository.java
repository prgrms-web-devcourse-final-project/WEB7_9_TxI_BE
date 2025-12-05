package com.back.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
