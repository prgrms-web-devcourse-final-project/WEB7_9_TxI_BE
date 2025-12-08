package com.back.support.factory;

import java.lang.reflect.Field;

import net.datafaker.Faker;

public class BaseFactory {

	protected static final Faker faker = new Faker();

	public static <T> T withId(T entity, Long id) {
		try {
			Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(entity, id);
			return entity;
		} catch (Exception e) {
			throw new RuntimeException(
				"Failed to set id on %s".formatted(entity.getClass().getSimpleName()), e);
		}
	}
}