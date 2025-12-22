package com.back.api.s3.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.s3.dto.response.PresignedUrlResponse;
import com.back.api.s3.service.S3PresignedService;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class S3ImageController {

	private final S3PresignedService s3PresignedService;


	// 이벤트 이미지 업로드용 Presigned URL 발급
	@PostMapping("/events/upload-url")
	public ApiResponse<PresignedUrlResponse> issueEventImageUploadUrl(
		@RequestParam String fileName
	) {
		return ApiResponse.ok(
			"이벤트 이미지 업로드 URL 발급",
			s3PresignedService.issueUploadUrl(fileName)

		);
	}
}
