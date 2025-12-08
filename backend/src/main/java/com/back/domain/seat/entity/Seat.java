package com.back.domain.seat.entity;

import com.back.domain.event.entity.Event;
import com.back.global.entity.BaseEntity;
import com.back.global.error.code.SeatErrorCode;
import com.back.global.error.exception.ErrorException;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "seats",
	uniqueConstraints = {
		@UniqueConstraint( // 등급별 좌석 코드 중복 방지,  VIP석 A1, R석 A1 중복 가능
			name = "uk_event_grade_seatcode",
			columnNames = {"event_id", "grade", "seat_code"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
	private Event event;    // TODO: 실제 Event 엔티티로 변경 필요

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
			throw new ErrorException(SeatErrorCode.SEAT_ALREADY_RESERVED);
		}
		this.seatStatus = SeatStatus.SOLD;
	}

	public void markAsReserved() {
		if (seatStatus != SeatStatus.AVAILABLE) {
			throw new ErrorException(SeatErrorCode.SEAT_ALREADY_SOLD);
		}
		this.seatStatus = SeatStatus.RESERVED;
	}

	@Builder
	public static Seat createSeat(Event event, String seatCode, SeatGrade grade, int price) {
		Seat seat = new Seat();
		seat.event = event;
		seat.seatCode = seatCode;
		seat.grade = grade;
		seat.price = price;
		seat.seatStatus = SeatStatus.AVAILABLE;
		return seat;
	}

	public void update(String seatCode, SeatGrade grade, int price, SeatStatus seatStatus) {
		this.seatCode = seatCode;
		this.grade = grade;
		this.price = price;
		this.seatStatus = seatStatus;
	}
}
