package com.back.domain.user.entity;

import java.time.LocalDateTime;

import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
	@SequenceGenerator(
		name = "user_seq",
		sequenceName = "user_seq",
		allocationSize = 100
	)
	private Long id;

	@Column(nullable = false, length = 100, unique = true)
	private String email;

	@Column(nullable = false, length = 20, unique = true)
	private String nickname;

	@Column(nullable = false)
	private String password;

	@Column(name = "birth_date")
	private LocalDateTime birthDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "deleted_status")
	private UserActiveStatus activeStatus;

	@Column(name = "deleted_at")
	private LocalDateTime deleteDate;
}
