package com.back.domain.queue.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.queue.entity.QueueEntry;
import com.back.domain.queue.entity.QueueEntryStatus;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

	long countByEvent_Id(Long eventId);

	long countByEvent_IdAndQueueEntryStatus(Long eventId, QueueEntryStatus status);

	Optional<QueueEntry> findByEvent_IdAndUser_Id(Long eventId, Long userId);

	long countByEvent_IdAndQueueRankLessThan(Long eventId, Integer queueRank);

	boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

	@Query("SELECT q FROM QueueEntry q "
		+ "WHERE q.queueEntryStatus = :status "
		+ "AND q.expiredAt IS NOT NULL "
		+ "AND q.expiredAt < :now "
	)
	List<QueueEntry> findExpiredEntries(
		@Param("status") QueueEntryStatus status,
		@Param("now") LocalDateTime now
	);

	@Query("SELECT q.user.id FROM QueueEntry q "
		+ "WHERE q.event.id = :eventId "
		+ "AND q.queueEntryStatus = 'WAITING' "
		+ "ORDER BY q.queueRank ASC"
	)
	List<Long> findTopNWaitingUsers(
		@Param("eventId") Long eventId,
		@Param("count") int count
	);

	@Query("SELECT MAX(q.queueRank) FROM QueueEntry q "
		+ "WHERE q.event.id = :eventId "
	)
	Optional<Long> findMaxRankInQueue(
		@Param("eventId") Long eventId
	);

}
