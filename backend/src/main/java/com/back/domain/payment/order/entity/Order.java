package com.back.domain.payment.order.entity;

import static com.back.domain.payment.order.entity.OrderStatus.*;

import java.time.LocalDateTime;

import com.back.domain.ticket.entity.Ticket;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket; // 주문과 1:1 매핑, 반드시 필요

	//private String pg_order_id

	private Long amount; // 일단 클라이언트로부터 받고, 추후 서버가 재가공하여 저장

	@Enumerated(EnumType.STRING)
	private OrderStatus status = PENDING;

	private String paymentKey; // Toss paymentKey

	private String orderKey;   // merchant_uid(UUID)

	private String orderNumber; // 주문번호 (예: WF4840318933)

	private LocalDateTime paidAt;

	public void markPaid(String paymentKey) {
		this.status = OrderStatus.PAID;
		this.paymentKey = paymentKey;
		this.paidAt = LocalDateTime.now();
	}

	public void markFailed() {
		this.status = OrderStatus.FAILED;
	}
}
