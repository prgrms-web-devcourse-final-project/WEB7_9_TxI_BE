package com.back.domain.queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.queue.entity.QueueEntry;

/**
 * - PostgreSQL 영구 저장
 * - 감사 로그 및 통계 조회
 * - Redis 장애 시 복구용
 */
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {


}
