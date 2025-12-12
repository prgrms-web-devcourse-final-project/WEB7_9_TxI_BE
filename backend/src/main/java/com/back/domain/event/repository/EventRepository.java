package com.back.domain.event.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {

	@Query("SELECT e FROM Event e WHERE e.id = :id AND e.deleted = false")
	Optional<Event> findById(@Param("id") Long id);

	@Query("SELECT e FROM Event e WHERE e.deleted = false ORDER BY e.createAt DESC")
	Page<Event> findAllByOrderByCreateAtDesc(Pageable pageable);

	@Query("SELECT e FROM Event e WHERE e.status = :status AND e.deleted = false ORDER BY e.createAt DESC")
	Page<Event> findByStatusOrderByCreateAtDesc(@Param("status") EventStatus status, Pageable pageable);

	@Query("SELECT e FROM Event e WHERE e.category = :category AND e.deleted = false ORDER BY e.createAt DESC")
	Page<Event> findByCategoryOrderByCreateAtDesc(@Param("category") EventCategory category, Pageable pageable);

	@Query("""
			SELECT e
			FROM Event e
			WHERE e.status = :status
			AND e.category = :category AND e.deleted = false
			ORDER BY e.createAt DESC
		""")
	Page<Event> findByStatusAndCategoryOrderByCreateAtDesc(
		@Param("status") EventStatus status,
		@Param("category") EventCategory category,
		Pageable pageable);

	@Query("SELECT e FROM Event e WHERE "
		+ "(:status IS NULL OR e.status = :status) AND "
		+ "(:category IS NULL OR e.category = :category) AND "
		+ "e.deleted = false "
		+ "ORDER BY e.createAt DESC")
	Page<Event> findByConditions(
		@Param("status") EventStatus status,
		@Param("category") EventCategory category,
		Pageable pageable);

	@Query("SELECT e FROM Event e WHERE e.status = :status AND e.deleted = false")
	List<Event> findByStatus(@Param("status") EventStatus status);

	@Query("""
			SELECT e
			FROM Event e
			WHERE e.ticketOpenAt
			BETWEEN :start AND :end AND e.status = :status AND e.deleted = false
		""")
	List<Event> findByTicketOpenAtBetweenAndStatus(
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end,
		@Param("status") EventStatus status
	);

	@Query("SELECT e FROM Event e WHERE e.status IN :statuses AND e.deleted = false")
	List<Event> findByStatusIn(@Param("statuses") List<EventStatus> statuses);

	@Query("""
			SELECT COUNT(e) > 0
			FROM Event e
			WHERE e.title = :title
			AND e.place = :place
			AND e.ticketOpenAt = :ticketOpenAt
			AND e.deleted = false
		""")
	boolean existsByTitleAndPlaceAndTicketOpenAtAndDeletedFalse(
		@Param("title") String title,
		@Param("place") String place,
		@Param("ticketOpenAt") LocalDateTime ticketOpenAt);

	@Query("""
			SELECT e
			FROM Event e
			WHERE e.title = :title
			AND e.place = :place
			AND e.ticketOpenAt = :ticketOpenAt
			AND e.deleted = false
		""")
	Optional<Event> findByTitleAndPlaceAndTicketOpenAtAndDeletedFalse(
		@Param("title") String title,
		@Param("place") String place,
		@Param("ticketOpenAt") LocalDateTime ticketOpenAt);
}
