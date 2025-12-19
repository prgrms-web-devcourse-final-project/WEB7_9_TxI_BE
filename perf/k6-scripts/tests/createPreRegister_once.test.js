import { createPreRegister } from "../scenarios/createPreRegister.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  // 각 VU가 정확히 1번만 실행
  scenarios: {
    preregister_once: {
      executor: 'per-vu-iterations',
      vus: parseInt(__ENV.VUS || "100", 10),
      iterations: 1,  // 각 VU당 1번만 실행
      maxDuration: '5m',  // 최대 5분 (안전장치)
    },
  },
};

/**
 * 사전등록 생성 부하 테스트 (1회 실행 버전)
 *
 * 목적:
 * - 각 사용자가 정확히 1번만 사전등록
 * - 시간 기반이 아닌 횟수 기반 테스트
 * - 중복 없이 깔끔한 데이터 생성
 *
 * 테스트 데이터 구조:
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - 각 사용자는 Event #5에 정확히 1번만 사전등록
 *
 * 이벤트:
 * - Event #5 (READY 상태)
 * - 사전등록 기간 DB에서 수정 필요
 *
 * SQL 수정:
 * UPDATE events
 * SET pre_open_at = NOW() - INTERVAL '1 day',
 *     pre_close_at = NOW() + INTERVAL '7 days'
 * WHERE id = 5;
 *
 * 시나리오:
 * - VU 1~500 각각 1번만 실행
 * - VU 1: 좌석 1 등록 → 종료
 * - VU 2: 좌석 2 등록 → 종료
 * - ...
 * - VU 500: 좌석 500 등록 → 종료
 *
 * 특징:
 * - 반복 없음 (1회 실행 후 종료)
 * - 모든 요청 201 Created (중복 없음)
 * - 빠른 실행 속도
 * - 깔끔한 테스트 데이터 생성
 *
 * 실행 예시:
 * VUS=500 k6 run perf/k6-scripts/tests/createPreRegister_once.test.js
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET 환경변수가 필요합니다.");
  }

  const vus = parseInt(__ENV.VUS || "100", 10);

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: vus }, (_, i) => {
    const userId = i + 1; // userId 1~vus
    return generateJWT(
      {
        id: userId,
        email: `test${userId}@test.com`,
        nickname: `PerfUser${userId}`
      },
      secret
    );
  });

  // 이벤트 ID 설정 (기본값: 5)
  const eventId = parseInt(__ENV.EVENT_ID || "5", 10);

  console.log(`Testing with ${vus} users (1 iteration each)`);
  console.log(`사전등록 생성 (1회 실행):`);
  console.log(`   - Event ID: ${eventId}`);
  console.log(`   - userId 1~${vus}`);
  console.log(`   - 각 VU가 정확히 1번만 실행`);
  console.log(`   - 예상 결과: 모든 요청 201 Created`);

  if (eventId === 5) {
    console.log(`Event #5 사용 (사전등록 기간 DB에서 수정 필요)`);
  }

  if (vus > 500) {
    console.warn(`VUS(${vus})가 사용자 수(500)보다 큽니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
    eventId,
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용
  const vuIndex = (__VU - 1) % data.tokens.length;
  const jwt = data.tokens[vuIndex];

  // 사전등록 (1번만 실행)
  createPreRegister(baseUrl, jwt, data.testId, data.eventId);
}
