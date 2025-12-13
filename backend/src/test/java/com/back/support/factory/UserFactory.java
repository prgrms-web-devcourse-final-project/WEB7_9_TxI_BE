package com.back.support.factory;

import java.time.LocalDate;
import java.time.Month;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserActiveStatus;
import com.back.domain.user.entity.UserRole;
import com.back.support.data.TestUser;

public class UserFactory extends BaseFactory {

	public static TestUser fakeUser(UserRole role, PasswordEncoder encoder) {

		String rawPassword = faker.internet().password();

		// 1960년 ~ 2010년 사이 랜덤 연도
		int year = faker.number().numberBetween(1960, 2010);

		// 1 ~ 12월 랜덤
		int monthValue = faker.number().numberBetween(1, 13);
		Month month = Month.of(monthValue);

		// 해당 월의 최대 일수 (윤년 자동 반영)
		int maxDay = month.length(java.time.Year.isLeap(year));

		// 1 ~ maxDay 랜덤
		int day = faker.number().numberBetween(1, maxDay + 1);

		LocalDate birthDate = LocalDate.of(year, monthValue, day);

		User user = User.builder()
			.activeStatus(UserActiveStatus.ACTIVE)
			.role(role)
			.email(faker.internet().emailAddress())
			.fullName(faker.lorem().characters(3, 6))
			.nickname(faker.lorem().characters(3, 6))
			.password(encoder.encode(rawPassword))
			.birthDate(birthDate)
			.build();

		return new TestUser(user, rawPassword);
	}
}
