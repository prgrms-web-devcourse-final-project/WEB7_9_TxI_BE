package com.back.support.helper;

import org.springframework.stereotype.Component;

import com.back.domain.store.entity.Store;
import com.back.domain.store.repository.StoreRepository;
import com.back.support.factory.StoreFactory;

@Component
public class StoreHelper {

	private final StoreRepository storeRepository;

	StoreHelper(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	public Store createStore() {
		return storeRepository.save(StoreFactory.fakeStore());
	}
}
