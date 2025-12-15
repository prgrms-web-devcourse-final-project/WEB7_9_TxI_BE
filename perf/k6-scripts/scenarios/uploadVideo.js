import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/challenge/rooms/{roomId}/videos
 * - youtubeUrl의 `v=` 파라미터가 그대로 videoId로 DB에 저장됨
 * - Stub이 extractVideoId()를 사용하므로 유니크 ID 생성이 핵심
 */
export function uploadVideo(baseUrl, jwt, testId, userId, roomId) {
  // 고유 videoId 생성 (특수문자 정리)
  const videoId = `dummy_${roomId}_${userId}_${testId}_${__ITER}`
    .replace(/[^A-Za-z0-9_-]/g, "_");
  const youtubeUrl = `https://www.youtube.com/watch?v=${videoId}`;

  const payload = JSON.stringify({ youtubeUrl });

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    tags: {
      api: "uploadVideo",
      test_id: testId,
      room_id: String(roomId),
      user_id: String(userId),
    },
  };

  const res = http.post(
    `${baseUrl}/api/v1/challenge/rooms/${roomId}/videos`,
    payload,
    params
  );

  let json;
  try {
    json = res.json();
  } catch {
    console.error(`❌ JSON parse error (user=${userId}, room=${roomId}):`, res.body);
    return res;
  }

  check(res, {
    "status 200": (r) => r.status === 200,
    "message present": (r) => r.body.includes("UPLOAD_SUCCESS"),
  });

  if (res.status !== 200) {
    console.warn(
      `⚠️ Non-200 status (${res.status}) user=${userId}, room=${roomId}, url=${youtubeUrl}`
    );
  }

  return res;
}