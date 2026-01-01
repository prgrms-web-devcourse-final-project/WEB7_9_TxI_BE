package com.back.global.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserRole;
import com.back.global.utils.JwtUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Slf4j
public class JwtProvider {

	@Value("${custom.jwt.secret}")
	private String secret;

	@Value("${custom.jwt.access-token-duration}")
	private long accessTokenDurationSeconds;

	@Value("${custom.jwt.refresh-token-duration}")
	private long refreshTokenDurationSeconds;

	private static final String CLAIM_ID = "id";
	private static final String CLAIM_STORE_ID = "storeId";
	private static final String CLAIM_NICKNAME = "nickname";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String CLAIM_JTI = "jti";
	private static final String CLAIM_SESSION_ID = "sid";
	private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

	public String generateAccessToken(User user, String sessionId, long tokenVersion) {
		return generateToken(user, "access", accessTokenDurationSeconds, sessionId, tokenVersion);
	}

	public String generateRefreshToken(User user, String sessionId, long tokenVersion) {
		return generateToken(user, "refresh", refreshTokenDurationSeconds, sessionId, tokenVersion);
	}

	private String generateToken(
		User user,
		String tokenType,
		long durationSeconds,
		String sessionId,
		long tokenVersion
	) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_ID, user.getId());

		if (user.getStore() != null && user.getStore().getId() != null) {
			claims.put(CLAIM_STORE_ID, user.getStore().getId());
		}

		claims.put(CLAIM_NICKNAME, user.getNickname());
		claims.put(CLAIM_ROLE, user.getRole().name());
		claims.put(CLAIM_TOKEN_TYPE, tokenType);
		claims.put(CLAIM_JTI, UUID.randomUUID().toString());
		claims.put(CLAIM_SESSION_ID, sessionId);
		claims.put(CLAIM_TOKEN_VERSION, tokenVersion);

		log.info("Generate Token({}), userId: {}", tokenType, user.getId());
		return JwtUtil.sign(secret, durationSeconds, claims);
	}

	/** access token 유효 기간 (ms 단위) */
	public long getAccessTokenValiditySeconds() {
		return accessTokenDurationSeconds;
	}

	/** refresh token 유효 기간 (ms 단위) */
	public long getRefreshTokenValiditySeconds() {
		return refreshTokenDurationSeconds;
	}

	public JwtClaims payloadOrNull(String jwt) {
		Map<String, Object> payload = JwtUtil.payloadOrNull(jwt, secret);

		if (payload == null || payload.isEmpty()) {
			return null;
		}

		Object idObj = payload.get("id");
		if (!(idObj instanceof Number idNo)) {
			return null;
		}

		Optional<Long> storeId = Optional.empty();
		Object storeIdObj = payload.get(CLAIM_STORE_ID);
		if (storeIdObj == null) {
			storeId = Optional.empty();
		} else if (storeIdObj instanceof Number storeIdNo) {
			storeId = Optional.of(storeIdNo.longValue());
		} else {
			log.error("JwtClaims storeId must be null or number, payload: {}", payload);
			return null;
		}

		String nickname = (String)payload.getOrDefault(CLAIM_NICKNAME, "");
		String tokenType = (String)payload.getOrDefault(CLAIM_TOKEN_TYPE, null);
		String jti = (String)payload.getOrDefault(CLAIM_JTI, null);
		String sid = (String)payload.getOrDefault(CLAIM_SESSION_ID, null);

		if (StringUtils.isBlank(sid) || StringUtils.isBlank(tokenType)) {
			return null;
		}

		long tokenVersion = 0L;
		Object tokenVersionObj = payload.get(CLAIM_TOKEN_VERSION);
		if (tokenVersionObj instanceof Number tokenVersionNo) {
			tokenVersion = tokenVersionNo.longValue();
		}

		UserRole role = UserRole.NORMAL;
		Object roleObj = payload.get(CLAIM_ROLE);
		if (roleObj instanceof String roleStr) {
			try {
				role = UserRole.valueOf(roleStr);
			} catch (Exception ignored) {
				return null;
			}
		} else if (roleObj instanceof UserRole userRole) {
			role = userRole;
		} else if (roleObj != null) {
			return null;
		}

		return new JwtClaims(
			idNo.longValue(),
			storeId,
			nickname == null ? "" : nickname,
			role,
			tokenType,
			jti,
			sid,
			tokenVersion
		);
	}

	public boolean isExpired(String jwt) {
		return JwtUtil.isExpired(jwt, secret);
	}
}
