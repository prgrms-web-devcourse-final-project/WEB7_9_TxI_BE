package com.back.api.s3.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.back.api.s3.dto.response.PresignedUrlResponse;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3PresignedService {

	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.presigned.put-expire-minutes}")
	private long putExpire;

	@Value("${cloud.aws.presigned.get-expire-minutes}")
	private long getExpire;

	// 이미지 업로드용 Presigned PUT
	public PresignedUrlResponse issueUploadUrl(String originalFileName) {

		// 확장자 추출
		String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

		String objectKey = "events/temp/" + UUID.randomUUID() + extension;

		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(objectKey)
			.build();

		PresignedPutObjectRequest presigned =
			s3Presigner.presignPutObject(
				PutObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(putExpire))
					.putObjectRequest(request)
					.build()
			);

		return new PresignedUrlResponse(presigned.url().toString(), objectKey);
	}

	// 이미지 조회용 Presigned GET
	public String issueDownloadUrl(String objectKey) {
		GetObjectRequest request = GetObjectRequest.builder()
			.bucket(bucket)
			.key(objectKey)
			.build();

		PresignedGetObjectRequest presigned =
			s3Presigner.presignGetObject(
				GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(getExpire))
					.getObjectRequest(request)
					.build()
			);

		return presigned.url().toString();
	}

}
