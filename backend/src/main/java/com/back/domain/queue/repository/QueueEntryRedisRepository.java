package com.back.domain.queue.repository;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class QueueEntryRedisRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	private static final String WAITING_KEY = "queue:%d:waiting"; //대기열
	private static final String ENTERED_KEY = "queue:%d:entered"; //입장 완료
	private static final String ENTERED_COUNT_KEY = "queue:%d:entered:count"; //입장 완료 수


	/* ==================== 대기열 관련 메서드 ==================== */

	// 대기열에 추가. rank 순번대로
	public void addToWaitingQueue(Long eventId, Long userId, int rank) {

		String key = String.format(WAITING_KEY, eventId);
		redisTemplate.opsForZSet().add(key, userId.toString(), rank);
		log.info("Added user to waiting queue - eventId: {}, userId: {}, rank: {}", eventId, userId, rank);
	}

	// 대기열에서 제거
	public void removeFromWaitingQueue(Long eventId, Long userId) {
		String key = String.format(WAITING_KEY, eventId);
		redisTemplate.opsForZSet().remove(key, userId.toString());
		log.info("Removed user from waiting queue - eventId: {}, userId: {}", eventId, userId);
	}

	public Long getMyRankInWaitingQueue(Long eventId, Long userId) {
		String key = String.format(WAITING_KEY, eventId);
		Long rank = redisTemplate.opsForZSet().rank(key, userId.toString());
		return rank != null ? rank + 1 : null; //0부터 시작하므로 +1
	}

	// 나보다 앞에 대기중인 사람 수
	public Long getWaitingAheadCount(Long eventId, Long userId) {
		Long rank = getMyRankInWaitingQueue(eventId, userId);
		return rank != null ? rank - 1 : null;
	}

	// 대기열 총 인원 수
	public Long getTotalWaitingCount(Long eventId) {
		String key = String.format(WAITING_KEY, eventId);
		Long size = redisTemplate.opsForZSet().size(key);
		return size != null ? size : 0L;
	}

	public Set<Object> getTopWaitingUsers(Long eventId, int count) {
		String key = String.format(WAITING_KEY, eventId);
		return redisTemplate.opsForZSet().range(key, 0, count - 1);
	}

	//userId와 rank 함께 전체 대기열 조회
	//broadCast에서 사용
	public Set<ZSetOperations.TypedTuple<Object>> getAllWaitingUsersWithRank(Long eventId) {
		String key = String.format(WAITING_KEY, eventId);
		return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
	}

	public boolean isInWaitingQueue(Long eventId, Long userId) {
		String key = String.format(WAITING_KEY, eventId);
		Double score = redisTemplate.opsForZSet().score(key, userId.toString());
		return score != null;
	}

	/* ==================== 입장 완료 관련 메서드 ==================== */

	public void moveToEnteredQueue(Long eventId, Long userId) {
		removeFromWaitingQueue(eventId, userId);
		String key = String.format(ENTERED_KEY, eventId);
		redisTemplate.opsForSet().add(key, userId.toString());

		redisTemplate.expire(key, java.time.Duration.ofMinutes(15)); //TTL 15분
		log.info("Moved user to entered queue - eventId: {}, userId: {}", eventId, userId);
	}

	public void removeFromEnteredQueue(Long eventId, Long userId) {
		String key = String.format(ENTERED_KEY, eventId);
		redisTemplate.opsForSet().remove(key, userId.toString());
		log.info("Removed user from entered queue - eventId: {}, userId: {}", eventId, userId);
	}

	public Long getTotalEnteredCount(Long eventId) {
		String key = String.format(ENTERED_KEY, eventId);
		Long size = redisTemplate.opsForSet().size(key);
		return size != null ? size : 0L;
	}

	public boolean isInEnteredQueue(Long eventId, Long userId) {
		String key = String.format(ENTERED_KEY, eventId);
		Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());
		return isMember != null && isMember;
	}

	/* ==================== 카운터 관련 메서드 ==================== */
	public Long incrementEnteredCount(Long eventId) {
		String key = String.format(ENTERED_COUNT_KEY, eventId);
		return redisTemplate.opsForValue().increment(key);
	}

	public Long getEnteredCount(Long eventId) {
		String key = String.format(ENTERED_COUNT_KEY, eventId);
		Object count = redisTemplate.opsForValue().get(key);
		return count != null ? (Long)count : 0L;
	}


	/* ==================== 임시 데이터 추가용 ==================== */
	public void addToEnteredQueueDirectly(Long eventId, Long userId) {
		String key = String.format(ENTERED_KEY, eventId);
		redisTemplate.opsForSet().add(key, userId.toString());
		redisTemplate.expire(key, java.time.Duration.ofMinutes(15));
	}

	/**
	 * ENTERED 카운트 직접 설정 (테스트 데이터용)
	 */
	public void setEnteredCount(Long eventId, int count) {
		String key = String.format(ENTERED_COUNT_KEY, eventId);
		redisTemplate.opsForValue().set(key, count);
	}

	/**
	 * 테스트용: 특정 이벤트의 모든 큐 데이터 삭제
	 */
	public void clearAll(Long eventId) {
		String waitingKey = String.format(WAITING_KEY, eventId);
		String enteredKey = String.format(ENTERED_KEY, eventId);
		String countKey = String.format(ENTERED_COUNT_KEY, eventId);

		redisTemplate.delete(waitingKey);
		redisTemplate.delete(enteredKey);
		redisTemplate.delete(countKey);

	}

}
