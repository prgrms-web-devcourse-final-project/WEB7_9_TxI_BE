package com.back.domain.queue.entity;

import java.time.LocalDateTime;

import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name="queue_entries")
public class QueueEntry extends BaseEntity {

	@Column(nullable = false)
	private int queueRank;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QueueEntryStatus queueEntryStatus;

	@Column(nullable = true)
	private LocalDateTime enteredAt;

	@Column(nullable = true)
	private LocalDateTime expiredAt;

	//TODO 유저, 이벤트 연관관계 추가 필요
}
