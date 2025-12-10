package com.back.domain.preregister.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.preregister.entity.PreRegisterStatus;

public interface PreRegisterRepository extends JpaRepository<PreRegister, Long> {

	boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

	Optional<PreRegister> findByEvent_IdAndUser_Id(Long eventId, Long userId);

	Long countByEvent_IdAndPreRegisterStatus(Long eventId, PreRegisterStatus status);
}
