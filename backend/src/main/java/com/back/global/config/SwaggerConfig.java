package com.back.global.config;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import com.back.global.config.swagger.ApiErrorCode;
import com.back.global.error.code.CommonErrorCode;
import com.back.global.error.code.ErrorCode;
import com.back.global.response.ApiResponse;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger 설정 및 API 문서에 에러 응답 자동 등록
 *
 * 동작 방식:
 * 1. 모든 API에 공통 에러(500) 자동 추가
 * 2. @ApiErrorCode 어노테이션이 있는 경우, 해당 도메인 에러만 추가
 * 3. 인터페이스와 구현체 모두에서 어노테이션 탐색
 */
@Configuration
public class SwaggerConfig {

	@Value("${custom.site.back-url}")
	private String backUrl;

	@Bean
	public OpenAPI openApi() {
		Server server = new Server();
		server.setUrl(backUrl);

		return new OpenAPI()
			.addServersItem(server)
			.info(new Info()
				.title("WaitFair API")
				.description("사전 추첨과 강화된 보안을 갖춘 차세대 스마트 예매 플랫폼")
				.version("v1.0.0"));
	}

	@Bean
	public OperationCustomizer operationCustomizer() {
		return (Operation operation, HandlerMethod handlerMethod) -> {
			// 1. 공통 에러 자동 등록 (모든 API에 500 에러 표시)
			addCommonErrors(operation);

			// 2. 도메인별 에러 등록 (@ApiErrorCode가 있는 경우만)
			ApiErrorCode apiErrorCode = findApiErrorCode(handlerMethod);
			if (apiErrorCode != null) {
				addDomainErrors(operation, apiErrorCode.value());
			}

			return operation;
		};
	}

	/**
	 * 공통 에러 자동 등록
	 * - 모든 API에 500 Internal Server Error 추가
	 */
	private void addCommonErrors(Operation operation) {
		addErrorExample(operation, CommonErrorCode.INTERNAL_SERVER_ERROR);
	}

	/**
	 * 도메인별 에러 등록
	 * - @ApiErrorCode에 명시된 ErrorCode 이름들을 찾아서 추가
	 */
	private void addDomainErrors(Operation operation, String[] errorCodeNames) {
		Arrays.stream(errorCodeNames)
			.map(this::findErrorCodeByName)
			.filter(errorCode -> errorCode != null)
			.forEach(errorCode -> addErrorExample(operation, errorCode));
	}

	/**
	 * ErrorCode를 Swagger Operation에 Example로 추가
	 *
	 * 작동 방식:
	 * 1. HTTP 상태 코드별로 ApiResponse 생성 (없으면 새로 만들고, 있으면 재사용)
	 * 2. 해당 ApiResponse의 MediaType에 ErrorCode Example 추가
	 */
	private void addErrorExample(Operation operation, ErrorCode errorCode) {
		String statusCode = String.valueOf(errorCode.getHttpStatus().value());

		// 상태 코드별 ApiResponse 생성 또는 가져오기
		io.swagger.v3.oas.models.responses.ApiResponse apiResponse =
			operation.getResponses().computeIfAbsent(statusCode, code ->
				createApiResponse(errorCode.getHttpStatus().getReasonPhrase())
			);

		// MediaType에 ErrorCode Example 추가
		MediaType mediaType = apiResponse.getContent().get("application/json");
		if (mediaType != null) {
			mediaType.addExamples(getErrorCodeName(errorCode), createExample(errorCode));
		}
	}

	/**
	 * ApiResponse 생성
	 */
	private io.swagger.v3.oas.models.responses.ApiResponse createApiResponse(String description) {
		io.swagger.v3.oas.models.responses.ApiResponse response =
			new io.swagger.v3.oas.models.responses.ApiResponse();
		response.setDescription(description);

		Content content = new Content();
		MediaType mediaType = new MediaType();
		content.addMediaType("application/json", mediaType);
		response.setContent(content);

		return response;
	}

	/**
	 * ErrorCode로부터 Swagger Example 생성
	 */
	private Example createExample(ErrorCode errorCode) {
		ApiResponse<?> errorResponse = ApiResponse.fail(errorCode);

		Example example = new Example();
		example.setValue(errorResponse);
		example.setDescription(errorCode.getMessage());

		return example;
	}

	/**
	 * ErrorCode 이름 추출 (enum name 또는 클래스 이름)
	 */
	private String getErrorCodeName(ErrorCode errorCode) {
		if (errorCode instanceof Enum<?>) {
			return ((Enum<?>)errorCode).name();
		}
		return errorCode.getClass().getSimpleName();
	}

	/**
	 * ErrorCode 이름으로 실제 ErrorCode enum 상수를 찾음
	 * - 모든 ErrorCode enum 클래스들을 탐색하여 일치하는 이름의 상수 반환
	 */
	private ErrorCode findErrorCodeByName(String errorCodeName) {
		// 탐색할 ErrorCode enum 클래스 목록
		Class<?>[] errorCodeClasses = {
			CommonErrorCode.class,
			com.back.global.error.code.AuthErrorCode.class,
			com.back.global.error.code.EventErrorCode.class,
			com.back.global.error.code.PreRegisterErrorCode.class,
			com.back.global.error.code.QueueEntryErrorCode.class,
			com.back.global.error.code.SeatErrorCode.class,
			com.back.global.error.code.TicketErrorCode.class,
			com.back.global.error.code.SmsErrorCode.class
		};

		for (Class<?> clazz : errorCodeClasses) {
			if (clazz.isEnum()) {
				try {
					Object[] constants = clazz.getEnumConstants();
					for (Object constant : constants) {
						if (constant instanceof Enum<?>) {
							Enum<?> enumConstant = (Enum<?>)constant;
							if (enumConstant.name().equals(errorCodeName)) {
								return (ErrorCode)enumConstant;
							}
						}
					}
				} catch (Exception e) {
					// 변환 실패 시 무시하고 다음 클래스 탐색
				}
			}
		}
		return null;
	}

	/**
	 * @ApiErrorCode 어노테이션 탐색
	 * - 구현체 메서드에서 먼저 찾고
	 * - 없으면 인터페이스 메서드에서 탐색
	 */
	private ApiErrorCode findApiErrorCode(HandlerMethod handlerMethod) {
		// 1. 구현체 메서드에서 찾기
		ApiErrorCode annotation =
			AnnotatedElementUtils.findMergedAnnotation(
				handlerMethod.getMethod(),
				ApiErrorCode.class
			);

		if (annotation != null) {
			return annotation;
		}

		// 2. 인터페이스 메서드에서 찾기
		Class<?> beanType = handlerMethod.getBeanType();
		Method implMethod = handlerMethod.getMethod();

		for (Class<?> iface : beanType.getInterfaces()) {
			try {
				Method interfaceMethod = iface.getMethod(
					implMethod.getName(),
					implMethod.getParameterTypes()
				);

				annotation = AnnotatedElementUtils.findMergedAnnotation(
					interfaceMethod,
					ApiErrorCode.class
				);

				if (annotation != null) {
					return annotation;
				}
			} catch (NoSuchMethodException e) {
				// 인터페이스에 해당 메서드가 없으면 무시
			}
		}

		return null;
	}
}
