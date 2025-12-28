package com.back.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findTop20ByUserIdOrderByCreateAtDesc(Long userId);

	long countByUserIdAndIsReadFalse(Long userId);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	List<Notification> findByUserIdAndIsReadFalse(Long userId);
}
