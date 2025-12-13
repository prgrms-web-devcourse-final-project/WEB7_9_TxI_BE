package com.back.support.factory;

import java.time.LocalDate;

import com.back.api.preregister.dto.request.PreRegisterCreateRequest;

public class PreRegisterRequestFactory extends BaseFactory {

	public static PreRegisterCreateRequest fakePreRegisterRequest(
		String nickname,
		String password,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			nickname,
			password,
			birthDate,
			true,
			true
		);
	}

	public static PreRegisterCreateRequest fakePreRegisterRequestWithoutTerms(
		String nickname,
		String password,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			nickname,
			password,
			birthDate,
			false,  // 이용약관 미동의
			true
		);
	}

	public static PreRegisterCreateRequest fakePreRegisterRequestWithoutPrivacy(
		String nickname,
		String password,
		LocalDate birthDate
	) {
		return new PreRegisterCreateRequest(
			nickname,
			password,
			birthDate,
			true,
			false  // 개인정보 수집 미동의
		);
	}
}
