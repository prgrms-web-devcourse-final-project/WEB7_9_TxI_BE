import { toggleCommentLike } from "../scenarios/toggleCommentLike.js";
import { generateJWT } from "../util/jwt.js";

export const options = {
  vus: parseInt(__ENV.VUS || "50", 10),
  duration: __ENV.DURATION || "20s",
};

/**
 * 각 VU에 대해 JWT + Room + Comment 매핑을 미리 생성
 * - PerfUser{i} → roomId = ((i - 1) % 15) + 1
 * - 각 room에 50개 댓글씩 있다고 가정 (총 15 × 50 = 750개)
 * → commentId = (roomId - 1) * 50 + 랜덤(1~50)
 */
export function setup() {
  const secret = __ENV.JWT_SECRET;

  const users = Array.from({ length: options.vus }, (_, i) => {
    const userId = i + 1;
    const roomId = ((userId - 1) % 15) + 1;
    const jwt = generateJWT({ id: userId, nickname: `PerfUser${userId}` }, secret);

    // 방별 댓글 구간 계산
    const baseCommentId = (roomId - 1) * 50;
    const commentId = baseCommentId + Math.floor(Math.random() * 50) + 1;

    return { jwt, userId, roomId, commentId };
  });

  return {
    users,
    testId: new Date().toISOString().replace(/[:.]/g, "-"),
  };
}

export default function (data) {
  const user = data.users[__VU - 1];
  const baseUrl = __ENV.BASE_URL || "http://host.docker.internal:8080";

  toggleCommentLike(baseUrl, user.jwt, data.testId, user.commentId);
}