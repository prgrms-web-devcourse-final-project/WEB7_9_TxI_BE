package com.back.global.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.domain.store.entity.Store;
import com.back.domain.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Order(0)
public class StoreDataInit implements ApplicationRunner {

	private final StoreRepository storeRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (storeRepository.count() > 0) {
			log.info("Store 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("Store 초기 데이터를 생성합니다.");

		Store store = Store.builder()
			.name("Test Store")
			.address("서울특별시 XX구 XX동 XX로")
			.registrationNumber("123-45-67890")
			.build();
		
		Store savedStore = storeRepository.save(store);

		log.info("storeId={} Store 데이터가 생성되었습니다.", savedStore.getId());
	}
}
