package com.back.api.s3.dto.response;

public record PresignedUrlResponse(
	String uploadUrl,
	String objectKey
) {
}
