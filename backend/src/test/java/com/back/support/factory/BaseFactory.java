package com.back.support.factory;

import java.lang.reflect.Field;

import net.datafaker.Faker;

public class BaseFactory {

	protected static final Faker faker = new Faker();

	public static <T> T withId(T entity, Long id) {
		try {
			Field idField = findField(entity.getClass(), "id");
			idField.setAccessible(true);
			idField.set(entity, id);
			return entity;
		} catch (Exception e) {
			throw new RuntimeException(
				"Failed to set id on %s".formatted(entity.getClass().getSimpleName()), e);
		}
	}

	private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null && current != Object.class) {
			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
