import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/comments/{commentId}/like
 * 특정 댓글에 좋아요/취소 요청을 보냄 (토글 방식)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자별 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (메트릭 태깅용)
 * @param {number} commentId - 좋아요할 댓글 ID
 */
export function toggleCommentLike(baseUrl, jwt, testId, commentId) {
  const url = `${baseUrl}/api/v1/comments/${commentId}/likes`;
  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "toggleCommentLike", test_id: testId, comment_id: commentId },
  };

  const res = http.post(url, null, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (comment=${commentId}):`, res.body);
    return res;
  }

  const data = json?.data ?? {};

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== undefined && data !== null,
    "has id": () => typeof data.id === "number",
    "likeCount valid": () => typeof data.likeCount === "number",
  });

  if (res.status >= 400) {
    console.warn(`⚠️ toggleCommentLike failed: ${res.status} ${res.body}`);
  }

  return res;
}