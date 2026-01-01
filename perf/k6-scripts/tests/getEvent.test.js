import { getEvent } from "../scenarios/getEvent.js";
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
 * 이벤트 단건 조회 부하 테스트
 * - 인증 필요 (JWT)
 * - 랜덤 eventId 조회로 다양한 이벤트 상세 페이지 조회 시뮬레이션
 * - 사용자가 목록에서 특정 이벤트를 클릭해서 상세 페이지를 보는 패턴
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

  return {
    tokens,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // VU별 고유 JWT 토큰 사용
  const jwt = data.tokens[(__VU - 1) % data.tokens.length];

  // 랜덤 이벤트 조회 (1~10 범위)
  // 실제 환경에 맞게 EVENT_ID_RANGE 환경변수로 조정
  const maxEventId = parseInt(__ENV.EVENT_ID_RANGE || "10", 10);
  const eventId = Math.floor(Math.random() * maxEventId) + 1;

  getEvent(baseUrl, jwt, data.testId, eventId);

  // 사용자가 이벤트 상세 페이지를 보는 시간 시뮬레이션
  sleep(1);
}
