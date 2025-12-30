package com.back.global.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("dev")
public class FlywayDevCleanMigrateConfig {

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(Environment env) {
		return flyway -> {
			String url = env.getProperty("spring.datasource.url", "");
			if (!url.contains("localhost") && !url.contains("127.0.0.1")) {
				throw new IllegalStateException("Refusing flyway.clean() on non-local DB: " + url);
			}
			flyway.clean();
			flyway.migrate();
		};
	}
}
