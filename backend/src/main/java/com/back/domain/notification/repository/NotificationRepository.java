package com.back.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.user.entity.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	// 최근 알림 20개
	List<Notification> findTop20ByUserOrderByCreatedAtDesc(User user);

	// 안 읽은 알림 개수
	long countByUserAndIsReadFalse(User user);

	// 로그인 유저 보호용 (본인 알림만 조회)
	Optional<Notification> findByIdAndUser(Long id, User user);
}
