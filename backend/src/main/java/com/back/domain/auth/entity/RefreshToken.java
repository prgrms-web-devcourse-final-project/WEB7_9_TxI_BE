package com.back.domain.auth.entity;

import java.time.LocalDateTime;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
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
	name = "refreshTokens",
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
	@GeneratedValue
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private String token;

	@Column(name = "issued_at")
	private LocalDateTime issuedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	private boolean revoked; // 기기 로그아웃 확인

	private String userAgent;

	private String ipAddress;

	@Builder
	private RefreshToken(
		User user,
		String token,
		LocalDateTime issuedAt,
		LocalDateTime expiresAt,
		boolean revoked,
		String userAgent,
		String ipAddress
	) {
		this.user = user;
		this.token = token;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.revoked = revoked;
		this.userAgent = userAgent;
		this.ipAddress = ipAddress;
	}

	public void revoke() {
		this.revoked = true;
	}

	public void updateRefreshToken(
		String newToken,
		LocalDateTime newIssuedAt,
		LocalDateTime newExpiresAt
	) {
		this.token = newToken;
		this.issuedAt = newIssuedAt;
		this.expiresAt = newExpiresAt;
	}
}
