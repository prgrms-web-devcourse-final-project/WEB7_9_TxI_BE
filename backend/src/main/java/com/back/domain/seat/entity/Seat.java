package com.back.domain.seat.entity;

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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Seat extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_seq")
	@SequenceGenerator(
		name = "seat_seq",
		sequenceName = "seat_seq",
		allocationSize = 100
	)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private MockEvent event;    // TODO: 실제 Event 엔티티로 변경 필요

	@Column(nullable = false, name = "seat_code")
	private String seatCode;  // 예시) "A1", "B2"

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "grade")
	private SeatGrade grade;

	@Column(nullable = false)
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "seat_status")
	private SeatStatus seatStatus;

	@Version // optimistic Locking을 위한 버전 필드
	private int version;

	public void markAsSold() {
		if (seatStatus != SeatStatus.AVAILABLE) {
			throw new IllegalStateException("Seat already taken");
		}
		this.seatStatus = SeatStatus.SOLD;
	}

	public void markAsReserved() {
		if (seatStatus != SeatStatus.AVAILABLE) {
			throw new IllegalStateException("Seat already taken");
		}
		this.seatStatus = SeatStatus.RESERVED;
	}
}
