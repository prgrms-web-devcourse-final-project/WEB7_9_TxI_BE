import encoding from "k6/encoding";
import crypto from "k6/crypto";

function sign(data, secretBase64) {
  const secretBytes = encoding.b64decode(secretBase64, "std");
  const hasher = crypto.createHMAC("sha256", secretBytes);
  hasher.update(data);
  return hasher.digest("base64")
    .replace(/\//g, "_").replace(/\+/g, "-").replace(/=/g, "");
}

export function generateJWT(payload, secretBase64) {
  if (!secretBase64) {
    throw new Error("JWT_SECRET is missing: pass it to generateJWT(payload, secret)");
  }
  const header = { alg: "HS256", typ: "JWT" };
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;

  const encodedHeader = encoding.b64encode(JSON.stringify(header), "rawurl");
  const encodedPayload = encoding.b64encode(JSON.stringify({ ...payload, iat, exp }), "rawurl");
  const signature = sign(`${encodedHeader}.${encodedPayload}`, secretBase64);
  const token = `${encodedHeader}.${encodedPayload}.${signature}`;

  if (__ENV.ENABLE_LOG === "true") {
    console.log(`✅ JWT for ${payload.nickname}: ${token.substring(0, 40)}...`);
  }
  return token;
}