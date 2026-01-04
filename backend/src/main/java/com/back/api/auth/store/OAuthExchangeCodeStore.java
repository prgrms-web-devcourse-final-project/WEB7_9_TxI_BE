package com.back.api.auth.store;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthExchangeCodeStore {

	private static final String KEY_PREFIX = "oauth:exchange:";
	private static final Duration TTL = Duration.ofSeconds(60);

	private final StringRedisTemplate stringRedisTemplate;

	public String issue(Long userId) {
		String code = UUID.randomUUID().toString().replace("-", "");
		String key = KEY_PREFIX + code;
		stringRedisTemplate.opsForValue().set(key, userId.toString(), TTL);
		return code;
	}

	public Optional<Long> consume(String code) {
		if (StringUtils.isBlank(code)) {
			return Optional.empty();
		}

		String key = KEY_PREFIX + code;

		String userIdStr = stringRedisTemplate.opsForValue().get(key);
		if (StringUtils.isBlank(userIdStr)) {
			return Optional.empty();
		}

		stringRedisTemplate.delete(key);
		return Optional.of(Long.parseLong(userIdStr));
	}
}
