import encoding from "k6/encoding";
import crypto from "k6/crypto";

function sign(data, secretBase64) {
  const secretBytes = encoding.b64decode(secretBase64, "std");
  const hasher = crypto.createHMAC("sha256", secretBytes);
  hasher.update(data);
  return hasher.digest("base64")
    .replace(/\//g, "_").replace(/\+/g, "-").replace(/=/g, "");
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function generateFixedSessionId(userId) {
  // userId 기반 고정 sessionId (PerfUserDataInitializer와 동일한 패턴)
  return `00000000-0000-0000-0000-${userId.toString().padStart(12, '0')}`;
}

export function generateJWT(payload, secretBase64) {
  if (!secretBase64) {
    throw new Error("JWT_SECRET is missing: pass it to generateJWT(payload, secret)");
  }
  const header = { alg: "HS256", typ: "JWT" };
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;

  // ActiveSession 필드 추가
  const userId = payload.id;
  const sessionId = generateFixedSessionId(userId);
  const tokenVersion = 1;
  const role = payload.role || "NORMAL";
  const tokenType = "access";
  const jti = generateUUID();

  const fullPayload = {
    id: userId,
    nickname: payload.nickname,
    email: payload.email,
    role: role,
    tokenType: tokenType,
    jti: jti,
    sid: sessionId,
    tokenVersion: tokenVersion,
    iat: iat,
    exp: exp
  };

  const encodedHeader = encoding.b64encode(JSON.stringify(header), "rawurl");
  const encodedPayload = encoding.b64encode(JSON.stringify(fullPayload), "rawurl");
  const signature = sign(`${encodedHeader}.${encodedPayload}`, secretBase64);
  const token = `${encodedHeader}.${encodedPayload}.${signature}`;

  if (__ENV.ENABLE_LOG === "true") {
    console.log(`✅ JWT for ${payload.nickname}: ${token.substring(0, 40)}...`);
  }
  return token;
}