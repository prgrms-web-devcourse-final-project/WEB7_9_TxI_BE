package com.back.domain.queue.entity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "queue_entries")
public class QueueEntry extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "queue_entry_seq")
	@SequenceGenerator(
		name = "queue_entry_seq",
		sequenceName = "queue_entry_seq",
		allocationSize = 100)
	private Long id;

	@Column(name="queue_rank", nullable = false)
	private int queueRank;

	@Enumerated(EnumType.STRING)
	@Column(name = "queue_entry_status", nullable = false)
	private QueueEntryStatus queueEntryStatus;

	@Column(name = "entered_at", nullable = true)
	private LocalDateTime enteredAt;

	@Column(name = "expired_at", nullable = true)
	private LocalDateTime expiredAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	public void enterQueue() {

		this.queueEntryStatus = QueueEntryStatus.ENTERED;
		this.enteredAt = LocalDateTime.now();
		this.expiredAt = this.enteredAt.plusMinutes(15); //시간 수정할 수도 있음

	}

	public void expire() {
		this.queueEntryStatus = QueueEntryStatus.EXPIRED;
	}

	//15분 초과 여부
	public boolean isExpired() {
		if(expiredAt == null) {
			return false;
		}
		return LocalDateTime.now().isAfter(this.expiredAt);
	}

}
