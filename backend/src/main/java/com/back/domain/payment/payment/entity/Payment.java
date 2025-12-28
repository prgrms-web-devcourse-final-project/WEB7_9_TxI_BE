package com.back.domain.payment.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class Payment {
	@Id
	@Column(name = "payment_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "paymentKey", nullable = false)
	private String paymentKey;

	@Column(name = "order_id", nullable = false)
	private String orderId; // 일단 엔티티간 연결 없음

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "method", nullable = false)
	private String method;

	@Column(name = "status", nullable = false)
	private ApproveStatus status;

	public Payment(String paymentKey, String orderId, Long amount, String method, ApproveStatus status) {
		this.paymentKey = paymentKey;
		this.orderId = orderId;
		this.amount = amount;
		this.method = method;
		this.status = status;
	}
}