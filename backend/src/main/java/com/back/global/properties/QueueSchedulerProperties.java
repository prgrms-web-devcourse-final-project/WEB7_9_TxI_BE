package com.back.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "queue.scheduler")
public class QueueSchedulerProperties {

	private Entry entry = new Entry();
	private Shuffle shuffle = new Shuffle();

	@Getter
	@Setter
	public static class Entry {
		private String cron;
		private int batchSize;
		private int maxEnteredLimit;
	}

	@Getter
	@Setter
	public static class Shuffle {
		private String cron;
		private int timeRangeMinutes;
	}

}
