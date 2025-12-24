package com.back.api.auth.dto.cache;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenCache implements Serializable {
	private String refreshTokenHash;
	private String sessionId;
	private long tokenVersion;
	private String jti; // JWT id
	private long issuedAtEpochMs;
}
