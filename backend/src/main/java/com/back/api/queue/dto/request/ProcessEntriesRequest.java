package com.back.api.queue.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "[테스트용] 대기열 입장 처리 요청 DTO")
public record ProcessEntriesRequest(
	@Schema(
		description = "입장 처리할 인원 수",
		example = "5",
		defaultValue = "1"
	)
	@Min(value = 1, message = "최소 1명 이상이어야 합니다.")
	Integer count
) {

	@JsonIgnore
	public int getCountOrDefault() {
		return count != null ? count : 1;
	}
}
