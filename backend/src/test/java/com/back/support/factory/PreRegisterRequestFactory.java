package com.back.support.factory;

import java.time.LocalDate;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;

public class PreRegisterRequestFactory extends BaseFactory {

	private static final String DEFAULT_PHONE_NUMBER = "01012345678";

	public static PreRegisterCreateRequest fakePreRegisterRequest(
		String fullName,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			fullName,
			DEFAULT_PHONE_NUMBER,
			birthDate,
			true,
			true
		);
	}

	public static PreRegisterCreateRequest fakePreRegisterRequest(
		String fullName,
		String phoneNumber,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			fullName,
			phoneNumber,
			birthDate,
			true,
			true
		);
	}

	public static PreRegisterCreateRequest fakePreRegisterRequestWithoutTerms(
		String fullName,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			fullName,
			DEFAULT_PHONE_NUMBER,
			birthDate,
			false,  // 이용약관 미동의
			true
		);
	}

	public static PreRegisterCreateRequest fakePreRegisterRequestWithoutPrivacy(
		String fullName,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			fullName,
			DEFAULT_PHONE_NUMBER,
			birthDate,
			true,
			false  // 개인정보 수집 미동의
		);
	}
}
