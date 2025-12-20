package com.back.domain.payment.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.back.domain.payment.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
}
