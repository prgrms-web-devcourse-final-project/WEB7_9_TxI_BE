import http from "k6/http";
import { check } from "k6";

/**
 * GET /api/v1/events?status={status}&category={category}&page={page}&size={size}
 * 이벤트 목록을 조회하는 시나리오 (공개 API, 인증 불필요)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} [page=0] - 페이지 번호
 * @param {number} [size=10] - 페이지당 아이템 수
 * @param {string} [status] - 이벤트 상태 필터 (READY, PRE_OPEN, QUEUE_READY, OPEN, CLOSED)
 * @param {string} [category] - 이벤트 카테고리 필터 (CONCERT, SPORTS, EXHIBITION, etc.)
 */
export function getEvents(baseUrl, testId, page = 0, size = 10, status = null, category = null) {
  let url = `${baseUrl}/api/v1/events?page=${page}&size=${size}`;

  if (status) {
    url += `&status=${status}`;
  }
  if (category) {
    url += `&category=${category}`;
  }

  const params = {
    headers: {
      Accept: "application/json",
    },
    tags: {
      api: "getEvents",
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

  // 서버 응답 구조: { message: string, data: Page<EventListResponse> }
  // Page 구조: { content: [...], pageable: {...}, totalElements, totalPages, ... }
  const data = json?.data ?? null;
  const content = data?.content ?? [];
  const totalElements = data?.totalElements ?? -1;

  check(res, {
    "status 200": (r) => r.status === 200,
    "data exists": () => data !== null,
    "content is array": () => Array.isArray(content),
    "totalElements valid": () => typeof totalElements === "number" && totalElements >= 0,
    "content length ≥ 0": () => content.length >= 0,
  });

  if (res.status !== 200) {
    console.error(`❌ getEvents failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      page,
      size,
    }));
  }

  return res;
}