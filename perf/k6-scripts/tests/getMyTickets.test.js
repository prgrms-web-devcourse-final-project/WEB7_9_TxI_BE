import { getMyTickets } from "../scenarios/getMyTickets.js";
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
 * 내 티켓 목록 조회 부하 테스트
 *
 * 목적:
 * - 사용자가 마이페이지에서 자신의 티켓 목록을 조회하는 패턴
 * - 인증된 사용자의 티켓 조회 성능 측정
 * - DB 조회 및 응답 속도 확인
 *
 * 테스트 데이터 구조 (Event #4 기준):
 * - 총 사용자: 500명 (test1@test.com ~ test500@test.com)
 * - Event #4: CLOSED 상태, 100석 (모두 ISSUED 티켓)
 * - 티켓 배정: 순환 배정 (i % users.size())
 *   → userId 1~100: 티켓 1장씩 보유 (Event #4)
 *   → userId 101~500: 티켓 없음 (빈 배열)
 *
 * 시나리오:
 * - 각 VU는 고유한 사용자로 자신의 티켓을 조회
 * - VU 1~100: 티켓 데이터 반환 (1장)
 * - VU 101~500: 빈 배열 반환
 * - 실제 사용자 행동 패턴 시뮬레이션
 *
 * 특징:
 * - 읽기 전용 작업으로 실패율 낮음
 * - DB 인덱스 성능 확인 (userId 기준 조회)
 * - 데이터 있음/없음 두 케이스 모두 테스트
 * - 캐싱 전략 검증에 유용
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

  // VU당 JWT 토큰 생성
  const tokens = Array.from({ length: maxVus }, (_, i) => {
    const userId = i + 1;
    return generateJWT(
      {
        id: userId,
        email: `test${userId}@test.com`,
        nickname: `PerfUser${userId}`
      },
      secret
    );
  });

  console.log(`Testing with ${maxVus} unique users`);
  console.log(`Event #4 티켓 보유 현황:`);
  console.log(`   - userId 1~100: 티켓 1장 보유 (ISSUED)`);
  console.log(`   - userId 101~${maxVus}: 티켓 없음 (빈 배열)`);

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용 (각 사용자는 자신의 티켓만 조회)
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  getMyTickets(baseUrl, jwt, data.testId);

  // 사용자가 티켓 목록을 확인하는 시간 (0.5~2.0초)
  sleep(Math.random() * 1.5 + 0.5);
}
