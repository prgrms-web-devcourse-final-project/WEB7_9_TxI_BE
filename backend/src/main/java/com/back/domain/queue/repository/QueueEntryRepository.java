package com.back.domain.queue.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;

/**
 * QueueEntry JPA Repository
 * - PostgreSQL 영구 저장
 * - 감사 로그 및 통계 조회
 * - Redis 장애 시 복구용
 */
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    // ==================== 기본 조회 ====================

    /**
     * 이벤트 + 사용자로 대기열 조회
     */
    Optional<QueueEntry> findByEventIdAndUserId(Long eventId, Long userId);

    /**
     * 대기열 존재 여부
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * 이벤트별 순번 정렬 조회
     */
    List<QueueEntry> findByEventIdOrderByQueueRank(Long eventId);

    // ==================== 상태별 조회 ====================

    /**
     * 이벤트별 특정 상태 대기열 조회
     */
    List<QueueEntry> findByEventIdAndQueueEntryStatus(Long eventId, QueueEntryStatus status);

    /**
     * 만료된 대기열 조회 (15분 초과)
     */
    List<QueueEntry> findByQueueEntryStatusAndExpiredAtBefore(
        QueueEntryStatus status,
        LocalDateTime dateTime
    );

    /**
     * WAITING 상태의 상위 N명 조회 (배치 입장용)
     */
    @Query("SELECT qe FROM QueueEntry qe " +
           "WHERE qe.eventId = :eventId " +
           "AND qe.queueEntryStatus = 'WAITING' " +
           "ORDER BY qe.queueRank")
    List<QueueEntry> findTopWaitingByEventId(
        @Param("eventId") Long eventId,
        Pageable pageable
    );

    // ==================== 카운트 및 통계 ====================

    /**
     * 이벤트별 상태별 카운트
     */
    long countByEventIdAndQueueEntryStatus(Long eventId, QueueEntryStatus status);

    /**
     * 전체 대기열 카운트
     */
    long countByEventId(Long eventId);

    /**
     * 특정 순번보다 작은 대기 인원 (내 앞 대기 인원 계산용)
     */
    @Query("SELECT COUNT(qe) FROM QueueEntry qe " +
           "WHERE qe.eventId = :eventId " +
           "AND qe.queueEntryStatus = 'WAITING' " +
           "AND qe.queueRank < :queueRank")
    long countWaitingAhead(
        @Param("eventId") Long eventId,
        @Param("queueRank") Integer queueRank
    );

    // ==================== 삭제 ====================

    /**
     * 이벤트 대기열 전체 삭제 (관리자 초기화용)
     */
    @Modifying
    @Query("DELETE FROM QueueEntry qe WHERE qe.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
