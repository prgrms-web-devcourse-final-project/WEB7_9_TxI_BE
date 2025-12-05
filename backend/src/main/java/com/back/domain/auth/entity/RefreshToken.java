package com.back.domain.auth.entity;

import java.time.LocalDateTime;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refreshTokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private String tokenHash;

	@Column(name = "issued_at")
	private LocalDateTime issuedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	private boolean revoked; // 기기 로그아웃 확인

	private String userAgent;

	private String ipAddress;
}
