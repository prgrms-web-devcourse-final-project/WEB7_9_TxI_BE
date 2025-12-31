package com.back.api.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.store.entity.Store;
import com.back.domain.store.repository.StoreRepository;
import com.back.global.error.code.StoreErrorCode;
import com.back.global.error.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

	private final StoreRepository storeRepository;

	public Store getStoreById(long storeId) {
		return storeRepository.findById(storeId).orElseThrow(
			() -> new ErrorException(StoreErrorCode.NOT_FOUND)
		);
	}
}
