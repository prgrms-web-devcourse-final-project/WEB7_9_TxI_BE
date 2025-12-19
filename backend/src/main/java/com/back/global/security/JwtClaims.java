package com.back.global.security;

import com.back.domain.user.entity.UserRole;

public record JwtClaims(
	long userId,
	String nickname,
	UserRole role,
	String tokenType,
	String jti,
	String sessionId,
	long tokenVersion
) {
}
