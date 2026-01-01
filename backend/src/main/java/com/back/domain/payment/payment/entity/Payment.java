package com.back.domain.payment.payment.entity;

import com.back.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payments")
public class Payment extends BaseEntity {
	@Id
	@Column(name = "payment_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_key", nullable = false)
	private String paymentKey;

	@Column(name = "order_id", nullable = false)
	private String orderId; // 일단 엔티티간 연결 없음

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "method", nullable = false)
	private String method;

	@Enumerated(EnumType.STRING)
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