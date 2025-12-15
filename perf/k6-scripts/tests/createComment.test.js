import { createComment } from "../scenarios/createComment.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  vus: parseInt(__ENV.VUS || "10", 10),
  duration: __ENV.DURATION || "10s",
};

/**
 * 각 VU에 대해 JWT + Room 매핑을 미리 생성
 * - PerfUser{i} → roomId = ((i - 1) % 15) + 1
 *   (즉, 1~15, 16~30, ... 식으로 15개 방에 고르게 분포)
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;

  const users = Array.from({ length: options.vus }, (_, i) => {
    const userId = i + 1;
    const roomId = ((userId - 1) % 15) + 1;
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId}` }, secret);

    return { jwt, roomId, userId };
  });

  return {
    users,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const user = data.users[__VU - 1];
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  // 각 유저가 자신이 속한 방에 댓글 작성
  createComment(baseUrl, user.jwt, data.testId, user.roomId);
}