package com.back.domain.payment.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.back.domain.payment.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	@Query("""
		SELECT o FROM Order o
		JOIN FETCH o.ticket t
		JOIN FETCH t.event e
		JOIN FETCH t.seat s
		WHERE o.id = :orderId
		""")
	Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
