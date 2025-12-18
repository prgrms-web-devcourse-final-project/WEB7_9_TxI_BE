package com.back.config;

import java.io.IOException;
import java.net.ServerSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@Configuration
@Profile("test")
public class TestRedisConfig {

	private RedisServer redisServer;
	private int redisPort;

	@PostConstruct
	public void startRedis() throws IOException {
		redisPort = findAvailableTcpPort();
		redisServer = new RedisServer(redisPort);
		redisServer.start();

		// Spring Boot Redis 설정에 반영
		System.setProperty("spring.data.redis.host", "localhost");
		System.setProperty("spring.data.redis.port", String.valueOf(redisPort));
		System.setProperty("spring.data.redis.password", "");

		System.out.printf("Embedded Redis 서버 시작 (port: %d)%n", redisPort);
	}

	@PreDestroy
	public void stopRedis() throws IOException {
		if (redisServer != null && redisServer.isActive()) {
			redisServer.stop();
			System.out.println("Embedded Redis 서버 종료");
		}
	}

	private int findAvailableTcpPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		}
	}
}
