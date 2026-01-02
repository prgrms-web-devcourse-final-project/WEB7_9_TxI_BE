package com.back.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 환경용 Redisson 설정
 * - TestRedisConfig의 embedded Redis 서버가 시작된 후에 Redisson 클라이언트 생성
 */
@Configuration
@Profile("test")
public class TestRedissonConfig {

	@Autowired
	private TestRedisConfig testRedisConfig;

	@Bean
	@DependsOn("testRedisConfig")
	public RedissonClient redissonClient() {
		Config config = new Config();

		// TestRedisConfig에서 System.setProperty로 설정한 값 사용
		String host = System.getProperty("spring.data.redis.host", "localhost");
		String port = System.getProperty("spring.data.redis.port", "6379");
		String address = "redis://" + host + ":" + port;

		config.useSingleServer()
			.setAddress(address)
			.setTimeout(200)
			.setConnectTimeout(200)
			.setRetryAttempts(0)  // fast-fail
			.setRetryInterval(0);

		return Redisson.create(config);
	}
}
