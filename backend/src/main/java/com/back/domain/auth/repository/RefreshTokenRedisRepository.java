package com.back.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.back.api.auth.dto.cache.RefreshTokenCache;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

	private static final String KEY_PREFIX = "auth:refresh:";
	private final @Qualifier("refreshTokenRedisTemplate")
	RedisTemplate<String, RefreshTokenCache> redisTemplate;

	private String key(long userId) {
		return KEY_PREFIX + userId;
	}

	public Optional<RefreshTokenCache> find(long userId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
	}

	public void save(long userId, RefreshTokenCache value, Duration ttl) {
		redisTemplate.opsForValue().set(key(userId), value, ttl);
	}

	public void delete(long userId) {
		redisTemplate.delete(key(userId));
	}
}
