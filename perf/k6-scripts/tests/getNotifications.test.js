import { getNotifications } from "../scenarios/getNotifications.js";
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
 * 알림 목록 조회 부하 테스트
 *
 * 목적:
 * - 사용자가 알림 목록을 조회하는 패턴
 * - 알림 목록 조회 성능 측정
 * - DB 조회 및 응답 속도 확인
 *
 * 테스트 데이터 구조:
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - 각 사용자는 자신의 알림 목록 조회
 * - 티켓 발급, 주문 성공/실패 등 다양한 타입의 알림
 *
 * 시나리오:
 * - VU 1~500 활성화 (모든 사용자 테스트 가능)
 * - 각 VU는 자신의 알림 목록을 조회
 * - 알림은 최신순으로 정렬되어 반환됨
 *
 * 특징:
 * - 읽기 전용 작업으로 실패율 낮음
 * - 본인의 알림만 조회 가능 (권한 검증)
 * - DB 리스트 조회 성능 확인
 * - 알림이 없는 경우에도 빈 배열 반환 (정상)
 *
 * 주의:
 * - PEAK_VUS는 500 이하로 설정 권장 (사용자가 500명만 존재)
 * - 500 초과 시에도 순환하여 테스트 가능
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
  console.log(`알림 목록 조회:`);
  console.log(`   - userId 1~${effectiveVus}`);

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

  getNotifications(baseUrl, jwt, data.testId);

  // 사용자가 알림 목록을 확인하는 시간 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);
}
