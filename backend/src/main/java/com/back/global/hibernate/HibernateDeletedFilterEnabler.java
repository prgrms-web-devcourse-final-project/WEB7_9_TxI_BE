package com.back.global.hibernate;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class HibernateDeletedFilterEnabler {

	@PersistenceContext
	private EntityManager entityManager;

	public void enable() {
		Session session = entityManager.unwrap(Session.class);
		session.enableFilter("deletedFilter");
	}
}
