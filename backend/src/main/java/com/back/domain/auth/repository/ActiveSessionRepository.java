package com.back.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.auth.entity.ActiveSession;

import jakarta.persistence.LockModeType;

public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ActiveSession s WHERE s.user.id = :userId")
	Optional<ActiveSession> findByUserIdForUpdate(@Param("userId") long userId);

	Optional<ActiveSession> findByUserId(long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ActiveSession a where a.user.id = :userId")
	void deleteByUserId(@Param("userId") long userId);
}
