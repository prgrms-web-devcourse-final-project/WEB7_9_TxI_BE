package com.back.domain.user.entity;

import java.time.LocalDate;
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
import lombok.Builder;
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

	@Column(nullable = false, name = "full_name", length = 30)
	private String fullName;

	@Column(nullable = false, length = 20, unique = true)
	private String nickname;

	@Column(nullable = false)
	private String password;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "deleted_status")
	private UserActiveStatus activeStatus;

	@Column(name = "deleted_at")
	private LocalDateTime deleteDate;

	@Builder
	public User(String email, String fullName, String nickname, String password,
		LocalDate birthDate, UserRole role, UserActiveStatus activeStatus) {
		this.email = email;
		this.fullName = fullName;
		this.nickname = nickname;
		this.password = password;
		this.birthDate = birthDate;
		this.role = role;
		this.activeStatus = activeStatus;
	}

	public User(Long id, String nickname) {
		this.id = id;
		this.nickname = nickname;
	}

	public void update(String fullName, String nickname, LocalDate birthDate) {
		this.fullName = fullName;
		this.nickname = nickname;
		this.birthDate = birthDate;
	}
}
