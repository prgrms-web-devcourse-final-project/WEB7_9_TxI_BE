package com.back.domain.preregister.entity;

import java.time.LocalDateTime;

import com.back.domain.event.entity.Event;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "pre_registers",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_pre_register_event_user",
			columnNames = {"event_id", "user_id"}
		)
	}
)
public class PreRegister extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pre_register_seq")
	@SequenceGenerator(
		name = "pre_register_seq",
		sequenceName = "pre_register_seq",
		allocationSize = 100
	)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "pre_register_status", nullable = false)
	private PreRegisterStatus preRegisterStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "pre_register_agree_terms", nullable = false)
	private Boolean preRegisterAgreeTerms;

	@Column(name = "pre_register_agree_privacy", nullable = false)
	private Boolean preRegisterAgreePrivacy;

	@Column(name = "pre_register_agreed_at", nullable = false)
	private LocalDateTime preRegisterAgreedAt;

	@Builder
	public PreRegister(Event event, User user,
		Boolean preRegisterAgreeTerms, Boolean preRegisterAgreePrivacy) {
		this.event = event;
		this.user = user;
		this.preRegisterStatus = PreRegisterStatus.REGISTERED;
		this.preRegisterAgreeTerms = preRegisterAgreeTerms;
		this.preRegisterAgreePrivacy = preRegisterAgreePrivacy;
		this.preRegisterAgreedAt = LocalDateTime.now();
	}

	public void cancel() {
		this.preRegisterStatus = PreRegisterStatus.CANCELED;
	}

	public void restore() {
		this.preRegisterStatus = PreRegisterStatus.REGISTERED;
	}

	public boolean isRegistered() {
		return this.preRegisterStatus == PreRegisterStatus.REGISTERED;
	}

	public boolean isCanceled() {
		return this.preRegisterStatus == PreRegisterStatus.CANCELED;
	}

	public Long getUserId() {
		return user.getId();
	}

	public Long getEventId() {
		return event.getId();
	}

}
