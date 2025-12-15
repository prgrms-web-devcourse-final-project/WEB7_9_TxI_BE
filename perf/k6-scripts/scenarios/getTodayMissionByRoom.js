import http from "k6/http";
import { check } from "k6";

// Perf 더미데이터 매핑 규칙:
// roomId = ((userId - 1) % 15) + 1
export function getTodayMission(baseUrl, jwt, userId, testId) {
  const roomId = ((userId - 1) % 15) + 1;

  const url = `${baseUrl}/api/v1/challenge/rooms/${roomId}/missions/today`;

  const res = http.get(url, {
    headers: {
      "Authorization": `Bearer ${jwt}`,
      "Content-Type": "application/json",
      "x-test-id": testId,
    },
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "valid response format": (r) => r.json("data") !== undefined,
  });

  return res;
}