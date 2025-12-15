import { sleep } from "k6";
import { generateJWT } from "../util/jwt.js";
import { getTodayMissionVideos } from "../scenarios/getTodayMissionVideos.js";

export const options = {
  vus: parseInt(__ENV.VUS || "100", 10),
  duration: __ENV.DURATION || "2m",
};

/**
 * PerfUser1~200 중 랜덤 선택
 * Room1~15 중 랜덤 조회
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  const MAX_USERS = 200;

  const users = Array.from({ length: MAX_USERS }, (_, i) => {
    const userId = i + 1;
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId}` }, secret);
    return { jwt, userId };
  });

  return {
    users,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";
  const user = data.users[Math.floor(Math.random() * data.users.length)];
  const roomId = 1 + Math.floor(Math.random() * 15);

  getTodayMissionVideos(baseUrl, user.jwt, data.testId, roomId);

  // 초당 2건 (read 부하)
  sleep(0.5);
}