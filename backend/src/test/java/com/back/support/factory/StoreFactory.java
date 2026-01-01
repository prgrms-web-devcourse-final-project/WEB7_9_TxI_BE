package com.back.support.factory;

import com.back.domain.store.entity.Store;

public class StoreFactory extends BaseFactory {

	public static Store fakeStore() {
		String name = faker.company().name();
		if (name.length() > 30) {
			name = name.substring(0, 30);
		}

		return Store.builder()
			.name(name)
			.registrationNumber(
				faker.number()
					.digits(16) // length 16
			)
			.address(faker.address().fullAddress())
			.build();
	}

	public static Store fakeStore(Long id) {
		Store store = fakeStore();
		return withId(store, id);
	}
}
