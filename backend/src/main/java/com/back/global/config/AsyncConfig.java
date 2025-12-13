package com.back.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/*
WebSocket 이벤트 발행을 비동기로 처리하여 메인 로직 블로킹 방지
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "taskExecutor")
	public Executor tastExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		//CPU 코어 수 2배, 5배로 -> 추후 테스트 후 변경
		executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 5);
		executor.setQueueCapacity(1000);
		executor.initialize();
		return executor;
	}
}
