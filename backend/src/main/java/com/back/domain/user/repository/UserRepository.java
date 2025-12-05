package com.back.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
