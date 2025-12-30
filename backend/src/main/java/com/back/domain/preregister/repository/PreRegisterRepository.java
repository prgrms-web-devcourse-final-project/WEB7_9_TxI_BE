package com.back.domain.preregister.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;

public interface PreRegisterRepository extends JpaRepository<PreRegister, Long> {

	@Query("SELECT pr.user.id FROM PreRegister pr "
		+ "WHERE pr.event.id = :eventId "
		+ "AND pr.preRegisterStatus = 'REGISTERED'")
	List<Long> findRegisteredUserIdsByEventId(@Param("eventId") Long eventId);

	long countByEvent_Id(Long eventId);

	boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

	Optional<PreRegister> findByEvent_IdAndUser_Id(Long eventId, Long userId);

	Long countByEvent_IdAndPreRegisterStatus(Long eventId, PreRegisterStatus status);

	List<PreRegister> findByUser_Id(Long userId);

	@Query("SELECT pr FROM PreRegister pr "
		+ "JOIN FETCH pr.user "
		+ "JOIN FETCH pr.event "
		+ "WHERE pr.event.id = :eventId "
		+ "AND pr.preRegisterStatus = 'REGISTERED' "
		+ "ORDER BY pr.createAt ASC"
	)
	Page<PreRegister> findByEventIdWithUserAndEvent(
		@Param("eventId") Long eventId,
		Pageable pageable
	);
}
