import { selectSeat } from "../scenarios/selectSeat.js";
import { generateJWT } from "../util/jwt.js";
import { sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },   // 10초 동안 50명으로 증가
    { duration: "1m", target: parseInt(__ENV.RAMP_UP_VUS || "50", 10) },     // 1분간 50명 유지
    { duration: "10s", target: parseInt(__ENV.PEAK_VUS || "100", 10) },      // 10초 동안 100명으로 증가 (피크)
    { duration: "1m", target: parseInt(__ENV.PEAK_VUS || "100", 10) },       // 1분간 100명 유지
    { duration: "1m", target: 0 },                                           // 1분 동안 0으로 감소
  ],
};

/**
 * 좌석 선택 부하 테스트 (원본 시나리오 - 혼합형)
 *
 * 목적:
 * - 실제 티켓팅 상황에 가장 가까운 시나리오
 * - 전체 좌석 풀에서 랜덤 선택 (500석)
 * - 시스템 전반적인 병목 지점 파악
 *
 * 방법:
 * - Event #3 (OPEN 상태, 500석) 타겟
 * - 좌석 범위: 1~500 (VIP: A1~A50, R: C1~C100, S: B1~B150, A: D1~D200)
 * - 사용자별 랜덤 좌석 선택
 *
 * 특징:
 * - 동일한 좌석을 여러 사용자가 동시에 선택하면 409 Conflict 발생 가능
 * - 이미 선택된 좌석을 선택하면 실패
 * - 시나리오 A (Non-Competitive)와 B (Competitive)의 중간 형태
 *
 * 비교:
 * - selectSeat_A.test.js: VU별 고유 좌석 할당 (경합 없음, 순수 로직 성능)
 * - selectSeat_B.test.js: 제한된 좌석으로 경합 (경쟁 비용 측정)
 * - selectSeat.test.js: 실제 티켓팅 혼합 시나리오 (본 파일)
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

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // Event #3 (OPEN 상태, 500석)
  const eventId = 3;

  // 좌석 ID 랜덤 선택 (1~500 범위)
  // 500석을 다수의 사용자가 경쟁적으로 선택하는 시나리오
  const totalSeats = 500;
  const seatId = Math.floor(Math.random() * totalSeats) + 1;

  selectSeat(baseUrl, jwt, data.testId, eventId, seatId);

  // 사용자 반응 시간 랜덤화 (0.5~2.0초)
  // 실제 사용자처럼 행동하여 Pending Thread 과장 방지
  sleep(Math.random() * 1.5 + 0.5);
}
