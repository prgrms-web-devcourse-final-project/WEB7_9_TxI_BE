import { getSeats } from "../scenarios/getSeats.js";
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
 * 좌석 목록 조회 부하 테스트
 * - 인증 필요 (JWT 토큰)
 * - 큐에 입장한 사용자만 조회 가능
 * - 사용자가 티켓팅 페이지에서 좌석을 조회하는 패턴
 *
 * 주의: 실제 환경에서는 큐 입장이 필요하므로,
 * 테스트 환경에서 큐 검증을 우회하거나 더미 데이터로 큐 입장 상태를 만들어야 함
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

  // 랜덤 이벤트 조회 (1~10 범위)
  const maxEventId = parseInt(__ENV.EVENT_ID_RANGE || "10", 10);
  const eventId = 3;
  //const eventId = Math.floor(Math.random() * maxEventId) + 1;

  getSeats(baseUrl, jwt, data.testId, eventId);

  // 사용자가 좌석 목록을 보고 선택하는 시간 시뮬레이션
  sleep(1);
}
