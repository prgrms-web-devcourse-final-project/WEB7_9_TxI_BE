package com.back.domain.queue.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

	long countByEvent_Id(Long eventId);

	long countByEvent_IdAndQueueEntryStatus(Long eventId, QueueEntryStatus status);

	Optional<QueueEntry> findByEvent_IdAndUser_Id(Long eventId, Long userId);

	long countByEvent_IdAndQueueRankLessThan(Long eventId, Integer queueRank);

	boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

}
