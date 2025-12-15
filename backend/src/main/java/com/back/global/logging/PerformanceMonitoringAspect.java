package com.back.global.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceMonitoringAspect {

	private final MeterRegistry meterRegistry;
	private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();

	@Around("execution(* com.back.api..controller..*(..)) || "
		+ "execution(* com.back.api..service..*(..))")
	// + "execution(* com.back.domain..repository..*(..))"
	public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.nanoTime();
		Object result = joinPoint.proceed();
		long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		String className = signature.getDeclaringType().getSimpleName();
		String methodName = signature.getName();
		String layer = getLayer(className);

		String key = layer + ":" + className + ":" + methodName;
		Timer timer = timerCache.computeIfAbsent(key, k ->
			Timer.builder("method.execution.time")
				.description("Method execution time (ms)")
				.tags("layer", layer, "class", className, "method", methodName)
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(meterRegistry)
		);
		timer.record(durationMs, TimeUnit.MILLISECONDS);

		log.debug("PERF layer={} class={} method={} durationMs={}", layer, className, methodName, durationMs);
		return result;
	}

	private String getLayer(String className) {
		String name = className.toLowerCase();
		if (name.contains("controller")) {
			return "controller";
		}
		if (name.contains("service")) {
			return "service";
		}
		if (name.contains("repository")) {
			return "repository";
		} else {
			return "other";
		}
	}
}
