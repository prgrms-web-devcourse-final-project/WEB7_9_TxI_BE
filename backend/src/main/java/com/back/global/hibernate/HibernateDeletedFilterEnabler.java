package com.back.global.hibernate;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class HibernateDeletedFilterEnabler {

	@PersistenceContext
	private EntityManager entityManager;

	// http요청-thread에서 한번만 실행 보장
	private static final ThreadLocal<Boolean> FILTER_ENABLED = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public void enable() {
		// 요청당 한 번만 필터 활성화
		if (Boolean.TRUE.equals(FILTER_ENABLED.get())) {
			return;
		}

		Session session = entityManager.unwrap(Session.class);
		session.enableFilter("deletedFilter");

		// 활성화 완료 표시
		FILTER_ENABLED.set(Boolean.TRUE);
	}

	public void clear() {
		FILTER_ENABLED.remove();
	}
}
