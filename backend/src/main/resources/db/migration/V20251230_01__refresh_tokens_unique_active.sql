-- user_id당 revoked=false(활성) refresh token 은 1개만 허용
CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_user_active
ON refresh_tokens (user_id)
WHERE revoked = false;