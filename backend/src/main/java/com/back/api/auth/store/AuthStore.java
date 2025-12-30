package com.back.api.auth.store;

import java.time.Duration;
import java.util.Optional;

import com.back.api.auth.dto.cache.RefreshTokenCache;

public interface AuthStore {
	Optional<RefreshTokenCache> findRefreshCache(long userId);

	void saveRefreshCache(long userId, RefreshTokenCache value, Duration ttl);

	void deleteRefreshCache(long userId);

	boolean isAvailable(); // redis 가 건강한지 확인
}
