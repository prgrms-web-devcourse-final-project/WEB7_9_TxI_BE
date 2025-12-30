-- refresh_tokens 테이블이 없으면 생성
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,

    token VARCHAR(512) NOT NULL,
    jti VARCHAR(36),

    issued_at TIMESTAMP,
    expires_at TIMESTAMP,

    session_id VARCHAR(36),
    token_version BIGINT,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    user_agent TEXT,
    ip_address TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 인덱스들 (없으면 생성)
CREATE INDEX IF NOT EXISTS idx_refresh_token_token
    ON refresh_tokens (token);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_revoked
    ON refresh_tokens (user_id, revoked);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_expires_at
    ON refresh_tokens (user_id, expires_at);

DO $$
BEGIN
  IF to_regclass('public.users') IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM pg_constraint WHERE conname = 'fk_refresh_tokens_user'
    ) THEN
      EXECUTE 'ALTER TABLE refresh_tokens
               ADD CONSTRAINT fk_refresh_tokens_user
               FOREIGN KEY (user_id) REFERENCES users(id)';
    END IF;
  END IF;
END $$;