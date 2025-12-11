package com.back.domain.payment.order.entity;

import static com.back.domain.payment.order.entity.OrderStatus.*;

import com.back.domain.event.entity.Event;
import com.back.domain.seat.entity.Seat;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class Order extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	//private String pg_order_id

	private Long amount; // 일단 클라이언트로부터 받고, 추후 서버가 재가공하여 저장

	@Enumerated(EnumType.STRING)
	private OrderStatus status = PENDING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event; //클라이언트로부터 받아야함

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user; //클라이언트로부터 받아야함 //v2에서는 직접 받지 않고 스프링시큐리티를 통해 JWT로부터 추출하는 방식으로 전환할 예정

	@OneToOne
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat; //클라이언트로부터 받아야함

}
