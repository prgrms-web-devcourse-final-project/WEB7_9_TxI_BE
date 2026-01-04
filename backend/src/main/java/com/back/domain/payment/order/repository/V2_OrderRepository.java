package com.back.domain.payment.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.back.domain.payment.order.entity.OrderStatus;
import com.back.domain.payment.order.entity.V2_Order;

@Repository
public interface V2_OrderRepository extends JpaRepository<V2_Order, String> {

	Optional<V2_Order> findByTicket_IdAndStatus(Long ticketId, OrderStatus status);
}
