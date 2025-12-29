package com.back.global.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TokenHash {
	public static String sha256(String raw) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digests = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder stringBuilder = new StringBuilder();

			for (byte digest : digests) {
				stringBuilder.append(String.format("%02x", digest));
			}

			return stringBuilder.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
