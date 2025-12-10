package com.back.domain.preregister.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사전등록 상태")
public enum PreRegisterStatus {

	@Schema(description = "사전등록 완료")
	REGISTERED,

	@Schema(description = "사전등록 취소")
	CANCELED
}
