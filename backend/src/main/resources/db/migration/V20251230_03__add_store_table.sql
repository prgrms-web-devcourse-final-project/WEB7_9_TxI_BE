/* =========================================================
 * 1. Sequence: store_seq
 * ========================================================= */
CREATE SEQUENCE IF NOT EXISTS store_seq
    START WITH 1
    INCREMENT BY 1
    CACHE 100;


/* =========================================================
 * 2. Table: stores
 * ========================================================= */
CREATE TABLE IF NOT EXISTS stores (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('store_seq'),
    name                VARCHAR(30)  NOT NULL,
    registration_number VARCHAR(16)  NOT NULL,
    address             TEXT         NOT NULL,
    deleted_at          TIMESTAMP NULL,

    created_at          TIMESTAMP NULL,
    modified_at         TIMESTAMP NULL
    );


/* =========================================================
 * 3. Partial Unique Index
 *    - deleted_at IS NULL 인 경우만 유니크
 * ========================================================= */
CREATE UNIQUE INDEX IF NOT EXISTS uk_store_registration_active
    ON stores (registration_number)
    WHERE deleted_at IS NULL;


/* =========================================================
 * 4. Alter: users - add store_id (NULL 허용)
 * ========================================================= */
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS store_id BIGINT NULL;


/* =========================================================
 * 5. Foreign Key: users.store_id → stores.id
 * ========================================================= */
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_user_store'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_user_store
            FOREIGN KEY (store_id)
            REFERENCES stores(id);
    END IF;
END $$;


/* =========================================================
 * 6. Index: users.store_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_users_store_id
    ON users(store_id);