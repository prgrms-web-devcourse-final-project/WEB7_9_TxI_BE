package com.back.global.config;

import java.time.Duration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Redisson 분산 락 설정
 * - 좌석 선택 동시성 제어용
 * - fast-fail 패턴: Redis 장애 시 즉시 실패 (DB fallback 없음)
 * - test 프로파일에서는 비활성화 (TestRedissonConfig 사용)
 */
@Configuration
@Profile("!test")
public class RedissonConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.data.redis.password:}")
	private String password;

	@Value("${spring.data.redis.timeout:200ms}")
	private Duration timeout;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		String address = "redis://" + host + ":" + port;

		config.useSingleServer()
			.setAddress(address)
			.setPassword(password.isBlank() ? null : password)
			.setTimeout((int) timeout.toMillis())
			.setConnectTimeout((int) timeout.toMillis())
			.setRetryAttempts(0)  // fast-fail: Redis 실패 시 재시도 없음
			.setRetryInterval(0);

		return Redisson.create(config);
	}
}
