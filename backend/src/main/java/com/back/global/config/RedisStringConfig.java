package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisStringConfig {
	@Bean
	public StringRedisTemplate stringRedisTemplate(
		RedisConnectionFactory connectionFactory
	) {
		return new StringRedisTemplate(connectionFactory);
	}
}
