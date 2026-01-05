/* =========================================================
 * users - provider / email 정책 반영
 * ========================================================= */

/* 1) provider_type, provider_id 컬럼 추가 */
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider_type VARCHAR(50);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

/* 2) 기존 row → provider_type = 'LOCAL' */
UPDATE users
SET provider_type = 'LOCAL'
WHERE provider_type IS NULL;


/* 3) provider_type 기본값 + NOT NULL */
ALTER TABLE users
    ALTER COLUMN provider_type SET DEFAULT 'LOCAL';

ALTER TABLE users
    ALTER COLUMN provider_type SET NOT NULL;


/* =========================================================
 * email partial unique
 * ========================================================= */

/* 4) 기존 email UNIQUE 인덱스 제거 */
DO
$$
    DECLARE
        idx_name text;
    BEGIN
        SELECT i.relname
        INTO idx_name
        FROM pg_class t
                 JOIN pg_index ix ON t.oid = ix.indrelid
                 JOIN pg_class i ON i.oid = ix.indexrelid
                 JOIN pg_attribute a ON a.attrelid = t.oid
        WHERE t.relname = 'users'
          AND ix.indisunique = true
          AND a.attname = 'email'
          AND a.attnum = ANY (ix.indkey)
        LIMIT 1;

        IF idx_name IS NOT NULL THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', idx_name);
        END IF;
    END
$$;

/* 5) email IS NOT NULL 인 경우만 유니크 */
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_not_null
    ON users (email)
    WHERE email IS NOT NULL;


/* =========================================================
 * provider unique (소셜 계정만)
 * ========================================================= */

/* 6) 소셜 계정에 대해서만 (provider_type, provider_id) 유니크 */
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_provider_social
    ON users (provider_type, provider_id)
    WHERE provider_type <> 'LOCAL';