package com.back.domain.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.domain.store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
