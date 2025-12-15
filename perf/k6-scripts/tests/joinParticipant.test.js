import { generateJWT } from "../util/jwt.js";
import { joinChallengeRoom } from "../scenarios/joinParticipant.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
  thresholds: {
    http_req_duration: ["p(95)<500"], // 발표용 기준 지표 유지
  },
};

/**
 * PerfUser1~N → Room16~215에 순차 매핑
 * - perf 더미 데이터 기준: 빈 방 ID가 16..215 (총 200개)
 * - VU가 200 초과여도 테스트는 돌아가지만, 200을 넘는 VU는 no-op 하도록 처리
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;

  const MAX_USERS = 200;
  const vus = parseInt(__ENV.VUS || "10", 10);
  const capped = Math.min(vus, MAX_USERS);

  const users = Array.from({ length: capped }, (_, i) => {
    const userId = i + 1;                 // PerfUser1..200 (더미 데이터와 일치)
    const roomId = 16 + i;                // Room16..215 (빈 방)
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId}` }, secret);
    return { jwt, userId, roomId };
  });

  return {
    users,
    maxUsers: capped,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";
  const vuIdx = __VU - 1;

  if (vuIdx >= data.maxUsers) return;

  // 각 유저는 join 1회만
  if (__ITER == 0) {
    const user = data.users[vuIdx];
    joinChallengeRoom(baseUrl, user.jwt, data.testId, user.roomId);
    sleep(0.5);
  }
}