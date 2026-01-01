package com.back.domain.payment.order.entity;

import static com.back.domain.payment.order.entity.OrderStatus.*;

import java.util.UUID;

import com.back.domain.payment.payment.entity.Payment;
import com.back.domain.ticket.entity.Ticket;
import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "v2_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class V2_Order extends BaseEntity {
	@Id
	@Column(name = "v2_order_id", length = 36)
	private String orderId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket; // 주문과 1:1 매핑, 반드시 필요

	@Column(name = "amount", nullable = false)
	private Long amount; // 일단 클라이언트로부터 받고, 추후 서버가 재가공하여 저장

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private OrderStatus status = PENDING;

	@Column(name = "payment_key", nullable = true)
	private String paymentKey; // TossPaymentKey

	@JoinColumn(name = "payment_id", nullable = true)
	@OneToOne(fetch = FetchType.LAZY)
	private Payment payment;

	@PrePersist
	public void generateOrderId() {
		if (this.orderId == null) {
			this.orderId = UUID.randomUUID().toString();
		}
	}

	public void markPaid(String paymentKey) {
		this.status = OrderStatus.PAID;
		this.paymentKey = paymentKey;
	}

	public void markFailed() {
		this.status = OrderStatus.FAILED;
	}
}
