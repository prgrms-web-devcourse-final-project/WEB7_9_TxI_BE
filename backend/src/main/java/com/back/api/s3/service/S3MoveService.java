package com.back.api.s3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

@Service
@RequiredArgsConstructor
public class S3MoveService {

	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;


	//temp 이미지 -> 영구 이미지로 이동
	public String moveImage(Long eventId, String tempKey) {

		String targetKey = "events/" + eventId + "/" + "/main.jpa";

		s3Client.copyObject(CopyObjectRequest.builder()
			.sourceBucket(bucket)
			.sourceKey(tempKey)
			.destinationBucket(bucket)
			.destinationKey(targetKey)
			.build());

		s3Client.deleteObject(builder -> builder
			.bucket(bucket)
			.key(tempKey)
			.build());

		return targetKey;
	}

}
