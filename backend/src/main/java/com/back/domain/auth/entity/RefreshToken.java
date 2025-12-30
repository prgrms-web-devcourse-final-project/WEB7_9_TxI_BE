package com.back.domain.auth.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "refresh_tokens",
	indexes = {
		@Index(name = "idx_refresh_token_token", columnList = "token"),
		@Index(name = "idx_refresh_token_user_revoked", columnList = "user_id, revoked"),
		@Index(name = "idx_refresh_token_user_expires_at", columnList = "user_id, expires_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token", nullable = false, length = 512)
	private String token;

	@Column(name = "jti", length = 36)
	private String jti;

	@Column(name = "issued_at")
	private LocalDateTime issuedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "session_id", length = 36)
	private String sessionId;

	@Column(name = "token_version")
	private Long tokenVersion;

	private boolean revoked; // 기기 로그아웃 확인

	private String userAgent;

	private String ipAddress;

	@Builder
	private RefreshToken(
		User user,
		String token,
		String jti,
		LocalDateTime issuedAt,
		LocalDateTime expiresAt,
		String sessionId,
		long tokenVersion,
		boolean revoked,
		String userAgent,
		String ipAddress
	) {
		this.user = user;
		this.token = token;
		this.jti = jti;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.sessionId = sessionId;
		this.tokenVersion = tokenVersion;
		this.revoked = revoked;
		this.userAgent = userAgent;
		this.ipAddress = ipAddress;
	}

	public void revoke() {
		this.revoked = true;
	}
}
