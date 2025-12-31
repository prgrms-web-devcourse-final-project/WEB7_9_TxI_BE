package com.back.global.security;

import java.util.Optional;

import com.back.domain.user.entity.UserRole;

public record JwtClaims(
	long userId,
	Optional<Long> storeId,
	String nickname,
	UserRole role,
	String tokenType,
	String jti,
	String sessionId,
	long tokenVersion
) {
}
