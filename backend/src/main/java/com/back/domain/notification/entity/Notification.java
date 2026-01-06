package com.back.domain.notification.entity;

import java.time.LocalDateTime;

import com.back.domain.notification.enums.DomainName;
import com.back.domain.notification.enums.NotificationTypeDetails;
import com.back.domain.notification.enums.NotificationTypes;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//TODO: User 엔티티쪽에 Cascade 설정 해줘야 함
@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "notifications")
public class Notification extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationTypes type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationTypeDetails typeDetail;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String message;

	@Column(nullable = false)
	private boolean isRead = false;

	@Column(nullable = true)
	private LocalDateTime readAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DomainName domainName;

	@Column(nullable = true)
	private Long domainId;

	//연관 필드
	@ManyToOne(fetch = FetchType.LAZY) // -> 리팩토링 고민요소
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public void markAsRead() {
		this.isRead = true;
		this.readAt = LocalDateTime.now();
	}
}
