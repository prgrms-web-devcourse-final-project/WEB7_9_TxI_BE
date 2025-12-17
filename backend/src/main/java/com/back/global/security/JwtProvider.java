package com.back.global.security;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.utils.JwtUtil;

import lombok.Getter;

@Component
@Getter
public class JwtProvider {

	@Value("${custom.jwt.secret}")
	private String secret;

	@Value("${custom.jwt.access-token-duration}")
	private long accessTokenDurationMillis;

	@Value("${custom.jwt.refresh-token-duration}")
	private long refreshTokenDurationMillis;

	private static final String CLAIM_ID = "id";
	private static final String CLAIM_NICKNAME = "nickname";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String CLAIM_JTI = "jti";

	public String generateAccessToken(User user) {
		Map<String, Object> claims = createBaseClaims(user);
		claims.put(CLAIM_TOKEN_TYPE, "access");
		claims.put(CLAIM_JTI, UUID.randomUUID().toString());

		return JwtUtil.toString(
			secret,
			accessTokenDurationMillis,
			claims
		);
	}

	public String generateRefreshToken(User user) {
		Map<String, Object> claims = createBaseClaims(user);
		claims.put(CLAIM_TOKEN_TYPE, "refresh");
		claims.put(CLAIM_JTI, UUID.randomUUID().toString());

		return JwtUtil.toString(
			secret,
			refreshTokenDurationMillis,
			claims
		);
	}

	private Map<String, Object> createBaseClaims(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_ID, user.getId());
		claims.put(CLAIM_NICKNAME, user.getNickname());
		claims.put(CLAIM_ROLE, user.getRole().name());
		return claims;
	}

	/** access token 유효 기간 (ms 단위) */
	public long getAccessTokenValidityMillis() {
		return accessTokenDurationMillis;
	}

	/** refresh token 유효 기간 (ms 단위) */
	public long getRefreshTokenValidityMillis() {
		return refreshTokenDurationMillis;
	}

	public Map<String, Object> payloadOrNull(String jwt) {
		Map<String, Object> payload = JwtUtil.payloadOrNull(jwt, secret);

		if (payload == null) {
			return null;
		}

		Object idObj = payload.get("id");
		if (!(idObj instanceof Number idNo)) {
			return null;
		}

		long id = idNo.longValue();

		String nickname = (String)payload.get("nickname");
		Object roleObj = payload.get("role");

		UserRole role = switch (roleObj) {
			case UserRole r -> r;
			case String s -> UserRole.valueOf(s); // "NORMAL" -> UserRole.NORMAL
			case null -> UserRole.NORMAL;
			default -> throw new IllegalStateException(
				"Unsupported role type in JWT payload: " + roleObj.getClass()
			);
		};

		return Map.of(
			CLAIM_ID, id,
			CLAIM_NICKNAME, nickname,
			CLAIM_ROLE, role
		);
	}

	public boolean isExpired(String jwt) {
		return JwtUtil.isExpired(jwt, secret);
	}
}
