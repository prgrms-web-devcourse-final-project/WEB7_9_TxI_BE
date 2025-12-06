package com.back.domain.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findByEventId(Long eventId);

	@Modifying
	@Query("DELETE FROM Seat s WHERE s.event.id = :eventId")
	void deleteByEventId(@Param("eventId") Long eventId);
}
