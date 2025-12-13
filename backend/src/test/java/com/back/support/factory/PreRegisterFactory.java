package com.back.support.factory;

import com.back.domain.event.entity.Event;
import com.back.domain.preregister.entity.PreRegister;
import com.back.domain.user.entity.User;

public class PreRegisterFactory extends BaseFactory {

	public static PreRegister fakePreRegister(Event event, User user) {
		return PreRegister.builder()
			.event(event)
			.user(user)
			.preRegisterAgreeTerms(true)
			.preRegisterAgreePrivacy(true)
			.build();
	}

	public static PreRegister fakeCanceledPreRegister(Event event, User user) {
		PreRegister preRegister = PreRegister.builder()
			.event(event)
			.user(user)
			.preRegisterAgreeTerms(true)
			.preRegisterAgreePrivacy(true)
			.build();
		preRegister.cancel();
		return preRegister;
	}
}
