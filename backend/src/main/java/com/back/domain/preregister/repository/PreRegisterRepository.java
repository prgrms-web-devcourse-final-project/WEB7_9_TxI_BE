package com.back.domain.preregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.preregister.entity.PreRegister;

public interface PreRegisterRepository extends JpaRepository<PreRegister, Long> {
}
