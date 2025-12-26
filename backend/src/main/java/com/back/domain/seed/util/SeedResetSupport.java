package com.back.domain.seed.util;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.back.domain.queue.repository.QueueEntryRedisRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeedResetSupport {

	private final JdbcTemplate jdbcTemplate;
	private final QueueEntryRedisRepository queueEntryRedisRepository;

	public void resetSeedDataByPrefix(String prefix) {
		// seed 이벤트 id 목록 수집
		List<Long> eventIds = jdbcTemplate.queryForList(
			"SELECT id from events WHERE title LIKE ?",
			Long.class,
			prefix + "%"
		);

		// Redis 키 정리
		for (Long eventId : eventIds) {
			queueEntryRedisRepository.clearAll(eventId);
		}

		// seed 데이터 삭제
		jdbcTemplate.update(
			"DELETE FROM pre_registers WHERE event_id in (SELECT id FROM events WHERE title LIKE ?)",
			prefix + "%"
		);

		jdbcTemplate.update(
			"DELETE FROM queue_entries WHERE event_id in (SELECT id FROM events WHERE title LIKE ?)",
			prefix + "%"
		);

		jdbcTemplate.update(
			"DELETE FROM events WHERE title LIKE ?",
			prefix + "%"
		);

		jdbcTemplate.update(
			"DELETE FROM users WHERE email LIKE ?",
			"seed%@test.com"
		);
	}
}
