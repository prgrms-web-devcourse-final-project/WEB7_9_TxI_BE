package com.back.domain.seat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.seat.entity.Seat;
import com.back.domain.seat.entity.SeatGrade;
import com.back.domain.seat.entity.SeatStatus;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	// 전체 좌석 조회
	@Query("""
			SELECT s
			FROM Seat s
			JOIN FETCH s.event
			WHERE s.event.id = :eventId
			ORDER BY s.grade ASC, s.seatCode ASC
		""")
	List<Seat> findAllSeatsByEventId(Long eventId);

	// Grade별 좌석 조회
	@Query("""
			SELECT s
			FROM Seat s
			JOIN FETCH s.event
			WHERE s.event.id = :eventId AND s.grade = :grade
			ORDER BY s.seatCode ASC
		""")
	List<Seat> findSeatsByEventIdAndGrade(Long eventId, SeatGrade grade);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update Seat s
		set s.seatStatus = :toStatus,
		    s.version = s.version + 1
		where s.event.id = :eventId
		and s.id = :seatId
		and s.seatStatus = :fromStatus
		""")
	int updateSeatStatusIfMatch(
		Long eventId,
		Long seatId,
		SeatStatus fromStatus,
		SeatStatus toStatus
	);

	// 성공 후 엔티티 다시 읽기용
	Optional<Seat> findByEventIdAndId(Long eventId, Long seatId);

	// 특정 이벤트의 특정 상태 좌석 조회 (성능 최적화)
	List<Seat> findByEventIdAndSeatStatus(Long eventId, SeatStatus seatStatus);

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

	// 관리자 대시보드용 - 이벤트별 특정 상태 좌석 수 조회
	Long countByEventIdAndSeatStatus(Long eventId, SeatStatus seatStatus);

	// 관리자 대시보드용 - 이벤트별 특정 상태 좌석의 총 판매 금액 조회
	@Query("SELECT COALESCE(SUM(s.price), 0) FROM Seat s WHERE s.event.id = :eventId AND s.seatStatus = :seatStatus")
	Long sumPriceByEventIdAndSeatStatus(@Param("eventId") Long eventId, @Param("seatStatus") SeatStatus seatStatus);

	@Query("""
		SELECT s
		FROM Seat s
		WHERE s.event.id = :eventId
		ORDER BY s.grade ASC,
				 REGEXP_REPLACE(s.seatCode, '[0-9]', '', 'g') ASC,
				 CAST(REGEXP_REPLACE(s.seatCode, '[^0-9]', '', 'g') AS INTEGER) ASC
		""")
	Page<Seat> findSortedSeatPageByEventIdForAdmin(
		@Param("eventId") Long eventId,
		Pageable pageable
	);

}
