package com.back.global.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {

	public static String sign(String secret, long durationSeconds, Map<String, Object> body) {
		SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()));

		ClaimsBuilder claimsBuilder = Jwts.claims();
		body.forEach(claimsBuilder::add);

		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + 1000L * durationSeconds);

		return Jwts.builder()
			.claims(claimsBuilder.build())
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();
	}

	public static Map<String, Object> payloadOrNull(String jwt, String secret) {
		Claims claims = claimsOrNull(jwt, secret);
		if (claims == null) {
			return null;
		}

		return new HashMap<>(claims);
	}

	public static boolean isExpired(String jwt, String secret) {
		try {
			claimsOrThrow(jwt, secret);
			return false;
		} catch (ExpiredJwtException e) {
			return true;
		} catch (Exception e) {
			return true;
		}
	}

	private static Claims claimsOrNull(String jwt, String secret) {
		try {
			return claimsOrThrow(jwt, secret);
		} catch (Exception e) {
			return null;
		}
	}

	private static Claims claimsOrThrow(String jwt, String secret) {
		SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()));
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(jwt)
			.getPayload();
	}
}
