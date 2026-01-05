package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig {

	// 고정 스케줄러 : QueueExpireScheduler, DraftTicketExpirationScheduler, QueueEntryScheduler
	@Bean(name = "fixedScheduler")
	@Primary // 기본 스케줄러로 지정
	public ThreadPoolTaskScheduler fixedScheduler() {

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5); // 고정 작업 2개 + 여유분 3개

		scheduler.setThreadNamePrefix("fixed-sched-");
		scheduler.initialize();
		return scheduler;
	}

	// 동적 스케줄러 : EventScheduler, QueueShuffleScheduler
	@Bean(name = "dynamicScheduler")
	public ThreadPoolTaskScheduler dynamicScheduler() {

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors() * 2); //cpu 코어 개수 2배로 설정

		scheduler.setThreadNamePrefix("dynamic-sched-");
		scheduler.initialize();
		return scheduler;
	}

	// 고정 스케줄러용
	@Bean
	public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
		return new RedisLockProvider(connectionFactory, "waitfair");
	}
}
