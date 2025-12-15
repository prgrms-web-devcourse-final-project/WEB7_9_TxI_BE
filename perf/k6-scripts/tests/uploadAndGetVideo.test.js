import { sleep } from "k6";
import { generateJWT } from "../util/jwt.js";
import { uploadVideo } from "../scenarios/uploadVideo.js";
import { getTodayMissionVideos } from "../scenarios/getTodayMissionVideos.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),  // 실행 시점에 env로 덮어쓸 수 있음
  duration: __ENV.DURATION || "10s",
};

/**
 * Setup:
 * - host(1) 제외, PerfUser1 → userId=2부터 시작
 * - roomId = ((userId - 1) % 15) + 1 (PerfDataInitializer 규칙 동일)
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;
  const vus = parseInt(__ENV.VUS || "10", 10);

  const users = Array.from({ length: vus }, (_, i) => {
    const userId = i + 2; // userId: 2..(vus+1)
    const roomId = ((userId - 1) % 15) + 1; // 1~15 순환 매핑
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId - 1}` }, secret);
    return { jwt, userId, roomId };
  });

  return {
    users,
    vus,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

/**
 * 시나리오:
 * - 첫 iteration: 업로드 요청 (중복 방지용 유니크 videoId)
 * - 이후 iteration: 동일 roomId 기준 today-mission 영상 조회 반복
 */
export default function (data) {
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";
  const userIdx = (__VU - 1) % data.vus;
  const user = data.users[userIdx];

  if (__ITER === 0) {
    uploadVideo(baseUrl, user.jwt, data.testId, user.userId, user.roomId);
  } else {
    getTodayMissionVideos(baseUrl, user.jwt, data.testId, user.roomId);
  }
}