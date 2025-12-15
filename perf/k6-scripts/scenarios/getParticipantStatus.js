import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/challenge/participants/{roomId}/status
 * 챌린지 참여 상태 조회 API 부하 테스트용
 *
 * @param {string} baseUrl - 서버 기본 주소
 * @param {string} jwt - JWT 토큰
 * @param {string} testId - 테스트 식별자 (태깅용)
 * @param {number} roomId - 조회할 방 ID
 */
export function getParticipationStatus(baseUrl, jwt, testId, roomId) {
  const url = `${baseUrl}/api/v1/challenge/participants/${roomId}/status`;
  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: { api: "getParticipationStatus", test_id: testId, room_id: roomId },
  });

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (room=${roomId}):`, res.body);
    return res;
  }

  check(res, {
    "status 200": (r) => r.status === 200,
    "valid response": () => json && typeof json.data?.joined === "boolean",
  });

  return res;
}