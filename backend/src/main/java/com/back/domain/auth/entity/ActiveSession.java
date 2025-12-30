package com.back.domain.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "active_sessions",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_active_session_user", columnNames = {"user_id"})
	},
	indexes = {
		@Index(name = "idx_active_session_session_id", columnList = "session_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveSession extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "token_version", nullable = false)
	private long tokenVersion;

	@Column(name = "last_login_at", nullable = false)
	private LocalDateTime lastLoginAt;

	@Builder
	private ActiveSession(
		User user,
		String sessionId,
		long tokenVersion,
		LocalDateTime lastLoginAt
	) {
		this.user = user;
		this.sessionId = sessionId;
		this.tokenVersion = tokenVersion;
		this.lastLoginAt = lastLoginAt;
	}

	public static ActiveSession create(User user) {
		return ActiveSession.builder()
			.user(user)
			.sessionId(UUID.randomUUID().toString())
			.tokenVersion(1L)
			.lastLoginAt(LocalDateTime.now())
			.build();
	}

	public void rotate() {
		this.sessionId = UUID.randomUUID().toString();
		this.tokenVersion += 1;
		this.lastLoginAt = LocalDateTime.now();
	}
}
