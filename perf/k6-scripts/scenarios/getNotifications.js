import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/notifications
 * 알림 목록 조회 시나리오 (인증 필요)
 *
 * 테스트 데이터:
 * - 사용자별 알림이 생성되어 있어야 함
 * - 티켓 발급, 주문 성공/실패 등 다양한 타입의 알림
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 */
export function getNotifications(baseUrl, jwt, testId) {
  const url = `${baseUrl}/api/v1/notifications`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
    },
    tags: {
      api: "getNotifications",
      test_id: testId,
    },
  };

  const res = http.get(url, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: List<NotificationResponseDto> }
  const data = json?.data ?? null;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "data is array": () => Array.isArray(data),
  });

  // 알림 목록 데이터 구조 검증
  if (res.status === 200 && Array.isArray(data) && data.length > 0) {
    const firstNotification = data[0];

    check(res, {
      "has id": () => typeof firstNotification?.id === "number",
      "has type": () => typeof firstNotification?.type === "string",
      "has typeDetail": () => typeof firstNotification?.typeDetail === "string",
      "has title": () => typeof firstNotification?.title === "string",
      "has message": () => typeof firstNotification?.message === "string",
      "has isRead": () => typeof firstNotification?.isRead === "boolean",
      "has createdAt": () => typeof firstNotification?.createdAt === "string",
    });
  }

  if (res.status !== 200) {
    console.error(`❌ getNotifications failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
    }));
  } else {
    // 성공 시 알림 개수 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      const notificationCount = Array.isArray(data) ? data.length : 0;
      const unreadCount = Array.isArray(data)
        ? data.filter(n => !n.isRead).length
        : 0;
      console.log(`✅ Notifications retrieved: total=${notificationCount}, unread=${unreadCount}`);
    }
  }

  return res;
}
