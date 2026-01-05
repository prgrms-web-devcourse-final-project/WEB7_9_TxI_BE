/* =========================================================
 * users - provider / email 정책 반영 (FIXED)
 * - 기존 email UNIQUE는 "인덱스"가 아니라 "제약조건(UNIQUE CONSTRAINT)"일 수 있어
 *   DROP INDEX가 아니라 DROP CONSTRAINT가 필요함.
 * - 환경별로 (1) UNIQUE CONSTRAINT, (2) UNIQUE INDEX 둘 다 케어
 * ========================================================= */

-- 1) provider_type, provider_id 컬럼 추가
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider_type VARCHAR(50);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- 2) 기존 row → provider_type = 'LOCAL'
UPDATE users
SET provider_type = 'LOCAL'
WHERE provider_type IS NULL;

-- 3) provider_type 기본값 + NOT NULL
ALTER TABLE users
    ALTER COLUMN provider_type SET DEFAULT 'LOCAL';

ALTER TABLE users
    ALTER COLUMN provider_type SET NOT NULL;


/* =========================================================
 * email unique 해제 -> partial unique로 전환
 * ========================================================= */

-- 4) 기존 email UNIQUE 해제
--    (A) UNIQUE CONSTRAINT 로 걸려있으면 CONSTRAINT를 제거
--    (B) UNIQUE INDEX 로만 걸려있으면 INDEX를 제거
DO
$$
    DECLARE
        con_name text;
        idx_name text;
    BEGIN
        /* (A) users 테이블의 UNIQUE 제약조건 중 (email) 을 포함하는 것 찾기 */
        SELECT c.conname
        INTO con_name
        FROM pg_constraint c
                 JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'users'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) ILIKE '%(email)%'
        LIMIT 1;

        IF con_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE users DROP CONSTRAINT IF EXISTS %I', con_name);
        END IF;

        /* (B) 혹시 constraint 없이 unique index로만 잡혀있는 케이스 */
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

-- 5) email IS NOT NULL 인 경우만 유니크
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_not_null
    ON users (email)
    WHERE email IS NOT NULL;


/* =========================================================
 * provider unique (소셜 계정만)
 * ========================================================= */

-- 6) 소셜 계정에 대해서만 (provider_type, provider_id) 유니크
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_provider_social
    ON users (provider_type, provider_id)
    WHERE provider_type <> 'LOCAL';