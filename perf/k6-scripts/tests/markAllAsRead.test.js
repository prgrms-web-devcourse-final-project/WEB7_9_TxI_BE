import { markAllAsRead } from "../scenarios/markAllAsRead.js";
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
 * 전체 알림 읽음 처리 부하 테스트
 *
 * 목적:
 * - 사용자가 모든 알림을 읽음 처리하는 패턴
 * - 전체 알림 읽음 처리 성능 측정
 * - DB 업데이트(UPDATE) 성능 확인
 *
 * 테스트 데이터 구조:
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - 각 사용자는 1~4번 이벤트에 대한 알림 보유
 * - 각 이벤트당 1~3개의 알림 (총 4~12개)
 * - 70% 확률로 이미 읽음 처리, 30%는 안 읽음
 *
 * 시나리오:
 * - VU 1~500 활성화 (모든 사용자 테스트 가능)
 * - 각 VU는 자신의 모든 알림을 읽음 처리
 * - 읽지 않은 알림이 없어도 요청 가능 (멱등성)
 *
 * 특징:
 * - 쓰기 작업으로 DB 부하가 있음
 * - 본인의 알림만 처리 가능 (권한 검증)
 * - 여러 번 호출해도 안전 (멱등성)
 * - 트랜잭션 처리 성능 확인
 *
 * 주의:
 * - PEAK_VUS는 500 이하로 설정 권장 (사용자가 500명만 존재)
 * - 500 초과 시에도 순환하여 테스트 가능
 * - 동일 사용자가 반복 호출하면 이미 읽음 처리된 상태
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

  console.log(`Testing with ${effectiveVus} users`);
  console.log(`전체 알림 읽음 처리:`);
  console.log(`   - userId 1~${effectiveVus}`);
  console.log(`   - 각 사용자의 모든 알림을 읽음 처리`);

  if (maxVus > userCount) {
    console.warn(`⚠️  PEAK_VUS(${maxVus})가 사용자 수(${userCount})보다 큽니다.`);
    console.warn(`   사용자 ID가 순환됩니다.`);
  }

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
    effectiveVus,
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용
  const vuIndex = (__VU - 1) % data.effectiveVus;
  const jwt = data.tokens[vuIndex];

  markAllAsRead(baseUrl, jwt, data.testId);

  // 사용자가 알림을 읽음 처리한 후 대기하는 시간 (1.0~3.0초)
  sleep(Math.random() * 2.0 + 1.0);
}
