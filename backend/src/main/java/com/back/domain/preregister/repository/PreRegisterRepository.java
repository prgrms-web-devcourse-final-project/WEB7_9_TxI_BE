package com.back.domain.preregister.repository;

<<<<<<< HEAD
import java.util.List;
=======
import java.util.Optional;
>>>>>>> ffb5874117a85203a12dffae47d8d57c60289e34

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.preregister.entity.PreRegister;

public interface PreRegisterRepository extends JpaRepository<PreRegister, Long> {

	@Query("SELECT pr.user.id FROM PreRegister pr "
		+ "WHERE pr.event.id = :eventId "
		+ "AND pr.preRegisterStatus = 'REGISTERED'")
	List<Long> findRegisteredUserIdsByEventId(@Param("eventId") Long eventId);

	long countByEvent_Id(Long eventId);

	boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

	Optional<PreRegister> findByEvent_IdAndUser_Id(Long eventId, Long userId);
}
