package com.back.domain.event.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.event.entity.Event;
import com.back.domain.event.entity.EventCategory;
import com.back.domain.event.entity.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {

	Page<Event> findAllByOrderByCreateAtDesc(Pageable pageable);

	Page<Event> findByStatusOrderByCreateAtDesc(EventStatus status, Pageable pageable);

	Page<Event> findByCategoryOrderByCreateAtDesc(EventCategory category, Pageable pageable);

	Page<Event> findByStatusAndCategoryOrderByCreateAtDesc(EventStatus status, EventCategory category,
		Pageable pageable);

	@Query("SELECT e FROM Event e WHERE "
		+ "(:status IS NULL OR e.status = :status) AND "
		+ "(:category IS NULL OR e.category = :category) "
		+ "ORDER BY e.createAt DESC")
	Page<Event> findByConditions(
		@Param("status") EventStatus status,
		@Param("category") EventCategory category,
		Pageable pageable);

	List<Event> findByStatus(EventStatus status);
}
