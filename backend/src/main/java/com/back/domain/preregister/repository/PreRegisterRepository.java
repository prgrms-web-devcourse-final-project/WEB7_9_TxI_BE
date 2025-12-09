package com.back.domain.preregister.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.preregister.entity.PreRegister;

public interface PreRegisterRepository extends JpaRepository<PreRegister, Long> {

	@Query("SELECT pr.user.id FROM PreRegister pr " +
		"WHERE pr.event.id = :eventId " +
		"AND pr.preRegisterStatus = 'REGISTERED'")
	List<Long> findRegisteredUserIdsByEventId(@Param("eventId") Long eventId);

	long countByEvent_Id(Long eventId);
}
