package com.back.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;

/**
 * Bucket4j Redis 설정
 *
 * Bucket4j를 Redis와 연동하여 분산 환경에서 Rate Limiting 구현
 */
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "security.bot-protection.rate-limit.enabled", havingValue = "true", matchIfMissing = false)
public class Bucket4jConfig {

	private RedisClient redisClient;
	private StatefulRedisConnection<String, byte[]> connection;

	/**
	 * Lettuce 기반 Bucket4j ProxyManager 생성
	 *
	 * @param redisConnectionFactory Redis Connection Factory
	 * @return LettuceBasedProxyManager
	 */
	@Bean
	public LettuceBasedProxyManager<String> lettuceBasedProxyManager(
		RedisConnectionFactory redisConnectionFactory
	) {
		LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory)redisConnectionFactory;
		RedisStandaloneConfiguration standaloneConfig = lettuceFactory.getStandaloneConfiguration();

		// Redis URI 생성 (비밀번호 포함)
		String redisUri;
		RedisPassword password = standaloneConfig.getPassword();
		if (password.isPresent()) {
			// redis://:{password}@{host}:{port} 형식
			redisUri = String.format("redis://:%s@%s:%d",
				new String(password.get()),
				standaloneConfig.getHostName(),
				standaloneConfig.getPort()
			);
		} else {
			// redis://{host}:{port} 형식
			redisUri = String.format("redis://%s:%d",
				standaloneConfig.getHostName(),
				standaloneConfig.getPort()
			);
		}

		// Redis Client 생성
		redisClient = RedisClient.create(redisUri);

		// Redis Connection 생성
		connection = redisClient.connect(
			RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
		);

		// ProxyManager 빌더 설정
		return LettuceBasedProxyManager.builderFor(connection)
			.withExpirationStrategy(
				ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
					java.time.Duration.ofMinutes(10) // Bucket 만료 시간
				)
			)
			.build();
	}

	/**
	 * Bean 소멸 시 Redis 리소스 정리
	 */
	@PreDestroy
	public void cleanup() {
		if (connection != null) {
			connection.close();
		}
		if (redisClient != null) {
			redisClient.shutdown();
		}
	}
}
