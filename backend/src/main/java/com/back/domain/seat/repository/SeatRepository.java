package com.back.domain.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findByEventId(Long eventId);

	@Modifying
	@Query("DELETE FROM Seat s WHERE s.event.id = :eventId")
	void deleteByEventId(@Param("eventId") Long eventId);

	@Query("""
			SELECT s.seatCode FROM Seat s
			WHERE s.event.id = :eventId
			AND s.grade = :grade
			AND s.seatCode IN :seatCodes
		""")
	List<String> findExistingSeatCodes(Long eventId, SeatGrade grade, List<String> seatCodes);

	@Query("""
		SELECT s.seatCode FROM Seat s
		WHERE s.event.id = :eventId
		  AND s.grade = :grade
		  AND s.seatCode = :seatCode
		  AND s.id <> :seatId
		""")
	List<String> findExistingSeatCodesExceptSelf(Long eventId, SeatGrade grade, String seatCode, Long seatId);
}
