package com.back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.back.api.auth.dto.cache.RefreshTokenCache;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.data.redis.password:}")
	private String password;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config =
			new RedisStandaloneConfiguration(host, port);

		if (password != null && !password.isBlank()) {
			config.setPassword(RedisPassword.of(password));
		}

		return new LettuceConnectionFactory(config);
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		return redisTemplate;
	}

	@Bean(name = "refreshTokenRedisTemplate")
	public RedisTemplate<String, RefreshTokenCache> refreshTokenRedisTemplate(
		RedisConnectionFactory connectionFactory
	) {
		RedisTemplate<String, RefreshTokenCache> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		StringRedisSerializer keySerializer = new StringRedisSerializer();
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

		template.setKeySerializer(keySerializer);
		template.setValueSerializer(valueSerializer);

		template.setHashKeySerializer(keySerializer);
		template.setHashValueSerializer(valueSerializer);

		template.afterPropertiesSet();
		return template;
	}

	//ActiveSession, QR용 RedisTemplate (String 직렬화)
	//필드 2개(sessionId, tokenVersion)만 저장하므로 경량 String 직렬화 사용
	//스프링부트에서 기본적으로 제공하는 StringRedisTemplate과 이름 충돌하기 때문에 다르게 작명
	@Bean(name = "stringTemplate")
	public RedisTemplate<String, String> stringTemplate(
		RedisConnectionFactory connectionFactory
	) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		StringRedisSerializer serializer = new StringRedisSerializer();

		template.setKeySerializer(serializer);
		template.setValueSerializer(serializer);
		template.setHashKeySerializer(serializer);
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}
}
