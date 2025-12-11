package com.back.global.security;

import java.util.Map;

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

	public String generateAccessToken(User user) {
		return JwtUtil.toString(
			secret,
			accessTokenDurationMillis,
			Map.of("id", user.getId(), "nickname", user.getNickname(), "role", user.getRole())
		);
	}

	public String generateRefreshToken(User user) {
		return JwtUtil.toString(
			secret,
			refreshTokenDurationMillis,
			Map.of("id", user.getId(), "nickname", user.getNickname(), "role", user.getRole())
		);
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

		return Map.of("id", id, "nickname", nickname, "role", role);
	}

	public boolean isExpired(String jwt) {
		return JwtUtil.isExpired(jwt, secret);
	}
}
