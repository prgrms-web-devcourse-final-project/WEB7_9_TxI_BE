import { sleep } from "k6";
import { generateJWT } from "../util/jwt.js";
import { getParticipationStatus } from "../scenarios/getParticipantStatus.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

/**
 * PerfUser1~200 → 실제로 참가해 있는 Room에 대해 status 조회
 * - PerfDataInitializer 기준, (userId - 1) % 15 + 1 = 참가중인 방 ID
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;

  const users = Array.from({ length: options.vus }, (_, i) => {
    const userId = i + 1;
    const roomId = ((userId - 1) % 15) + 1; // ✅ 실제 참여 중인 room
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId}` }, secret);
    return { jwt, userId, roomId };
  });

  return {
    users,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";
  const userIdx = (__VU - 1) % data.users.length;
  const user = data.users[userIdx];

  // 각 사용자가 실제로 참가한 roomId에 대해 status 조회
  getParticipationStatus(baseUrl, user.jwt, data.testId, user.roomId);

  sleep(0.5);
}