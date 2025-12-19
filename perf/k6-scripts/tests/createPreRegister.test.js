import { createPreRegister } from "../scenarios/createPreRegister.js";
import { generateJWT } from "../util/jwt.js";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },
    { duration: "1m", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },
    { duration: "10s", target: parseInt(__ENV.PEAK_VUS || "100", 10) },
    { duration: "1m", target: parseInt(__ENV.PEAK_VUS || "100", 10) },
    { duration: "1m", target: 0 },
  ],
};

/**
 * 사전등록 생성 부하 테스트
 *
 * 목적:
 * - 사용자가 이벤트에 사전등록하는 패턴
 * - 사전등록 생성 성능 측정
 * - 중복 등록 예외 처리 검증
 * - DB INSERT 성능 및 유니크 제약 조건 확인
 *
 * 테스트 데이터 구조:
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - 각 사용자는 지정된 이벤트에 사전등록
 * - 중복 등록 시 409 Conflict (정상 처리)
 *
 * 이벤트 선택:
 * - 기본값: Event #5 (READY 상태)
 * - EVENT_ID 환경변수로 변경 가능
 * - Event #1은 이미 1~500번 유저가 사전등록 완료 상태
 *
 *  Event #5 사용 시 주의사항:
 * Event #5는 READY 상태이며, 사전등록 기간이 다음과 같이 설정됩니다:
 * - preOpenAt: 현재 시점 + 2일
 * - preCloseAt: 현재 시점 + 9일
 *
 * Event #5를 사용하려면 먼저 DB에서 사전등록 기간을 수정해야 합니다:
 *
 * SQL 수정 예시:
 * UPDATE events
 * SET pre_open_at = NOW() - INTERVAL '1 day',
 *     pre_close_at = NOW() + INTERVAL '7 days'
 * WHERE id = 5;
 *
 * 시나리오:
 * - VU 1~500 활성화 (최대 500명 사용자)
 * - 각 VU는 자신의 계정으로 사전등록
 * - 첫 시도: 201 Created (성공)
 * - 반복 시도: 400 Bad Request "이미 사전등록되어 있습니다." (중복, 정상 처리)
 *
 * 특징:
 * - 쓰기 작업으로 DB 부하 있음
 * - 유니크 제약 조건 (event_id, user_id) 검증
 * - 멱등성 테스트 (중복 등록 방어)
 * - 본인만 등록 가능 (권한 검증)
 *
 * 주의:
 * - PEAK_VUS는 500 이하로 설정 권장 (사용자가 500명만 존재)
 * - 500 초과 시에도 순환하여 테스트 가능
 * - 반복 실행 시 대부분 409 Conflict 응답
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET 환경변수가 필요합니다.");
  }

  const maxVus = Math.max(
    parseInt(__ENV.RAMP_UP_VUS || "50", 10),
    parseInt(__ENV.PEAK_VUS || "100", 10)
  );

  // 사용자 1~500까지 토큰 생성
  const userCount = 500;
  const effectiveVus = Math.min(maxVus, userCount);

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: effectiveVus }, (_, i) => {
    const userId = i + 1; // userId 1~500
    return generateJWT(
      {
        id: userId,
        email: `test${userId}@test.com`,
        nickname: `PerfUser${userId}`
      },
      secret
    );
  });

  // 이벤트 ID 설정 (기본값: 5, 환경변수로 변경 가능)
  const eventId = parseInt(__ENV.EVENT_ID || "5", 10);

  console.log(`Testing with ${effectiveVus} users`);
  console.log(`사전등록 생성:`);
  console.log(`   - Event ID: ${eventId}`);
  console.log(`   - userId 1~${effectiveVus}`);
  console.log(`   - 중복 등록 시 400 응답 (정상 처리)`);

  if (eventId === 5) {
    console.log(`Event #5 사용 (사전등록 기간 DB에서 수정 필요)`);
    console.log(`   SQL: UPDATE events SET pre_open_at = NOW() - INTERVAL '1 day', pre_close_at = NOW() + INTERVAL '7 days' WHERE id = 5;`);
  } else if (eventId === 1) {
    console.warn(`Event #1은 이미 1~500번 유저가 사전등록 완료 상태입니다.`);
  }

  if (maxVus > userCount) {
    console.warn(`PEAK_VUS(${maxVus})가 사용자 수(${userCount})보다 큽니다.`);
    console.warn(`   사용자 ID가 순환됩니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
    effectiveVus,
    eventId,
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용
  const vuIndex = (__VU - 1) % data.effectiveVus;
  const jwt = data.tokens[vuIndex];

  createPreRegister(baseUrl, jwt, data.testId, data.eventId);

  // 사용자가 사전등록 후 대기하는 시간 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);
}
