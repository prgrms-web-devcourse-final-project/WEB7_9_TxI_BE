package com.back.api.s3.controller;

import org.springframework.web.bind.annotation.RequestParam;

import com.back.api.s3.dto.response.PresignedUrlResponse;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "S3 Image API", description = "S3 이미지 업로드 API")
public interface S3ImageApi {

	@Operation(
		summary = "이미지 업로드용 Presigned URL 발급",
		description =  """
			이벤트 이미지를 S3에 업로드하기 위한 Presigned PUT URL을 발급합니다.
	
			- 클라이언트는 이 API로 발급받은 URL을 사용해 S3에 직접 PUT 업로드합니다.
			- 업로드 성공 후 반환되는 objectKey를 이벤트 생성 API에 전달해야 합니다.
			- 업로드 URL은 일정 시간 후 만료됩니다.
			"""
	)
	ApiResponse<PresignedUrlResponse> issueEventImageUploadUrl(
		@Parameter(
			description = "업로드할 이미지 파일명 (확장자 포함)",
			example = "img.jpg"
		)
		@RequestParam String fileName
	);
}
