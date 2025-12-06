package com.back.domain.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
