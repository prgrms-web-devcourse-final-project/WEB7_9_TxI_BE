/* ============================================================
 * 1. events 테이블에 store_id 컬럼 추가 (초기에는 NULL 허용)
 * ============================================================ */
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

/* ============================================================
 * 2. Legacy Store 생성 (없으면 생성, 있으면 재사용)
 * ============================================================ */
DO
$$
    DECLARE
        v_store_id BIGINT;
    BEGIN
        SELECT id
        INTO v_store_id
        FROM stores
        WHERE name = 'Legacy Store'
        LIMIT 1;

        IF v_store_id IS NULL THEN
            INSERT INTO stores (id,
                                name,
                                registration_number,
                                address,
                                deleted_at,
                                created_at,
                                modified_at)
            VALUES (nextval('store_seq'),
                    'Legacy Store',
                    '000-00-00000',
                    'LEGACY',
                    NULL,
                    NOW(),
                    NOW())
            RETURNING id INTO v_store_id;
        END IF;

        /* ========================================================
         * 3. 기존 이벤트 → Legacy Store로 backfill
         * ======================================================== */
        UPDATE events
        SET store_id = v_store_id
        WHERE store_id IS NULL;

        /* ========================================================
         * 4. 기존 ADMIN 사용자 → Legacy Store로 backfill
         *    - store_id가 NULL인 경우만
         *    - deleted_at IS NULL (소프트 삭제 제외)
         * ======================================================== */
        UPDATE users
        SET store_id = v_store_id
        WHERE role = 'ADMIN'
          AND store_id IS NULL
          AND deleted_at IS NULL;
    END
$$;

/* ============================================================
 * 5. events.store_id NOT NULL 강제
 * ============================================================ */
ALTER TABLE events
    ALTER COLUMN store_id SET NOT NULL;

/* ============================================================
 * 6. FK 제약 추가
 * ============================================================ */
ALTER TABLE events
    ADD CONSTRAINT fk_events_store
        FOREIGN KEY (store_id) REFERENCES stores (id);

ALTER TABLE users
    ADD CONSTRAINT fk_user_store
        FOREIGN KEY (store_id) REFERENCES stores (id);

/* ============================================================
 * 7. 인덱스 (조회/조인 성능)
 * ============================================================ */
CREATE INDEX IF NOT EXISTS idx_events_store_id
    ON events (store_id);

CREATE INDEX IF NOT EXISTS idx_users_store_id
    ON users (store_id);
