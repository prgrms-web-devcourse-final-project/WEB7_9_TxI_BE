package com.back.api.s3.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.api.s3.dto.response.PresignedUrlResponse;
import com.back.api.s3.service.S3PresignedService;
import com.back.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminS3ImageController implements AdminS3ImageApi {

	private final S3PresignedService s3PresignedService;

	// 이벤트 이미지 업로드용 Presigned URL 발급
	@Override
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
