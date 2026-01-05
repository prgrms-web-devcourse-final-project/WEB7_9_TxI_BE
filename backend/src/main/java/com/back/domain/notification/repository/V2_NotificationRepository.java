package com.back.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.notification.entity.V2_Notification;

public interface V2_NotificationRepository extends JpaRepository<V2_Notification, Long> {
	List<V2_Notification> findTop20ByUserIdOrderByCreateAtDesc(Long userId);

	long countByUserIdAndIsReadFalse(Long userId);

	Optional<V2_Notification> findByIdAndUserId(Long notificationId, Long userId);

	List<V2_Notification> findByUserIdAndIsReadFalse(Long userId);
}
