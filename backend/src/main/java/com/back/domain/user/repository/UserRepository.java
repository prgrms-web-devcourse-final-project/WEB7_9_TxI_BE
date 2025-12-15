package com.back.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<User> findByEmail(String mail);

	Optional<User> findByEmailAndDeleteDateIsNull(String email);

	@Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
	Optional<User> findIncludingDeletedById(@Param("id") Long id);
}
