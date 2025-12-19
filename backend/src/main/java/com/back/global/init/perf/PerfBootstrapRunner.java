package com.back.global.init.perf;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 부하테스트용 데이터 초기화 중앙 통제
 *
 * 실행 예시:
 * ./gradlew bootRun --args='--users=1000 --events=10 --prereg-ratio=0.8 --queue-ratio=0.7 --ticket-ratio=0.3'
 */
@Component
@Profile("perf")
@RequiredArgsConstructor
@Slf4j
public class PerfBootstrapRunner implements ApplicationRunner {

	private final PerfUserDataInitializer userInit;
	private final PerfEventDataInitializer eventInit;
	private final PerfSeatDataInitializer seatInit;
	private final PerfPreregisterDataInitializer preregisterInit;
	private final PerfQueueDataInitializer queueInit;
	private final PerfTicketDataInitializer ticketInit;
	private final PerfNotificationDataInitializer notificationInit;

	@Override
	public void run(ApplicationArguments args) {
		// 파라미터 파싱 (기본값 포함)
		int userCount = getIntArg(args, "users", 500);
		int eventCount = getIntArg(args, "events", 50);
		double preregRatio = getDoubleArg(args, "prereg-ratio", 1);
		double queueRatio = getDoubleArg(args, "queue-ratio", 1);
		double ticketRatio = getDoubleArg(args, "ticket-ratio", 1);

		log.info("""
				
				=====================================
				🚀 부하테스트 데이터 초기화 시작
				=====================================
				📊 설정값:
				  - 사용자 수: {}명
				  - 이벤트 수: {}개
				  - 사전등록 비율: {}%
				  - 대기열 진입 비율: {}%
				  - 티켓 발급 비율: {}%
				=====================================
				""",
			userCount,
			eventCount,
			(int)(preregRatio * 100),
			(int)(queueRatio * 100),
			(int)(ticketRatio * 100)
		);

		// 순차 실행 (의존성 순서)
		log.info("1️⃣  User 데이터 생성 중...");
		userInit.init(userCount);

		log.info("2️⃣  Event 데이터 생성 중...");
		eventInit.init(eventCount);

		log.info("3️⃣  Seat 데이터 생성 중...");
		seatInit.init();

		log.info("4️⃣  PreRegister 데이터 생성 중...");
		preregisterInit.init(preregRatio);

		log.info("5️⃣  QueueEntry 데이터 생성 중...");
		queueInit.init(queueRatio);

		log.info("6️⃣  Ticket 데이터 생성 중...");
		ticketInit.init(ticketRatio);

		log.info("7️⃣  Notification 데이터 생성 중...");
		notificationInit.init();

		log.info("""

			=====================================
			✅ 부하테스트 데이터 초기화 완료
			=====================================
			""");
	}

	private int getIntArg(ApplicationArguments args, String key, int defaultValue) {
		if (args.containsOption(key)) {
			try {
				return Integer.parseInt(args.getOptionValues(key).get(0));
			} catch (NumberFormatException e) {
				log.warn("잘못된 숫자 형식: --{}={}, 기본값 {} 사용", key, args.getOptionValues(key).get(0),
					defaultValue);
				return defaultValue;
			}
		}
		return defaultValue;
	}

	private double getDoubleArg(ApplicationArguments args, String key, double defaultValue) {
		if (args.containsOption(key)) {
			try {
				return Double.parseDouble(args.getOptionValues(key).get(0));
			} catch (NumberFormatException e) {
				log.warn("잘못된 숫자 형식: --{}={}, 기본값 {} 사용", key, args.getOptionValues(key).get(0),
					defaultValue);
				return defaultValue;
			}
		}
		return defaultValue;
	}
}
