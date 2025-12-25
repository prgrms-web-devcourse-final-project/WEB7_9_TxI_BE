package com.back.domain.payment.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.payment.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}