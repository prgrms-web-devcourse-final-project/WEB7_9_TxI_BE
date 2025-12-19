import http from "k6/http";
import { check } from "k6";

/**
 * POST /api/v1/events/{eventId}/pre-registers
 * 사전등록 생성 시나리오 (인증 필요)
 *
 * 테스트 데이터:
 * - Event #5 사용 (테스트 콘서트 5)
 * - 사전등록 기간 확인 필요 (preOpenAt ~ preCloseAt)
 *
 * @param {string} baseUrl - API 기본 주소
 * @param {string} jwt - 사용자 JWT 토큰
 * @param {string} testId - 테스트 실행 ID (로그/메트릭 태깅용)
 * @param {number} eventId - 이벤트 ID
 */
export function createPreRegister(baseUrl, jwt, testId, eventId) {
  const url = `${baseUrl}/api/v1/events/${eventId}/pre-registers`;

  const params = {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    tags: {
      api: "createPreRegister",
      test_id: testId,
      event_id: eventId,
    },
  };

  const res = http.post(url, null, params);

  let json;
  try {
    json = res.json();
  } catch {
    console.error("❌ JSON parse error:", res.body);
    return res;
  }

  // 서버 응답 구조: { message: string, data: PreRegisterResponse }
  const data = json?.data ?? null;
  const message = json?.message || "";

  // ✅ 201 (성공) 또는 400/409 (중복 등록)을 모두 성공으로 처리
  // 백엔드에서 중복 등록 시 400 상태 코드와 "이미 사전등록되어 있습니다." 메시지 반환
  const isDuplicateError = (res.status === 400 || res.status === 409) && message.includes("이미 사전등록");
  const isSuccess = res.status === 201 || isDuplicateError;

  check(res, {
    "status 201 or duplicate (400/409)": () => isSuccess,
    "has data (if 201)": () => res.status !== 201 || data !== null,
    "has id (if 201)": () => res.status !== 201 || typeof data?.id === "number",
    "has eventId (if 201)": () => res.status !== 201 || typeof data?.eventId === "number",
    "has userId (if 201)": () => res.status !== 201 || typeof data?.userId === "number",
    "has status (if 201)": () => res.status !== 201 || typeof data?.status === "string",
    "has createdAt (if 201)": () => res.status !== 201 || typeof data?.createdAt === "string",
  });

  if (res.status === 201) {
    // 성공 시 사전등록 정보 로깅 (디버깅용)
    if (__ENV.DEBUG === "true") {
      console.log(`✅ PreRegister created: id=${data?.id}, eventId=${data?.eventId}, status=${data?.status}`);
    }
  } else if (isDuplicateError) {
    // 중복 등록 (정상 처리)
    if (__ENV.DEBUG === "true") {
      console.log(`ℹ️  PreRegister already exists for eventId=${eventId} (duplicate ignored)`);
    }
  } else {
    // 기타 에러
    console.error(`❌ createPreRegister failed [${res.status}]:`, JSON.stringify({
      status: res.status,
      message: json?.message,
      eventId,
    }));
  }

  return res;
}
