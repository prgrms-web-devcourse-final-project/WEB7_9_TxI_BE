/* =========================================================
 * 1. Table: payment
 * ========================================================= */
CREATE TABLE IF NOT EXISTS payment (
    payment_id      BIGSERIAL PRIMARY KEY,
    payment_key     VARCHAR(255) NOT NULL,
    order_id        VARCHAR(255) NOT NULL,
    amount          BIGINT       NOT NULL,
    method          VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,

    created_at      TIMESTAMP NULL,
    modified_at     TIMESTAMP NULL
);


/* =========================================================
 * 2. Alter: payment - add created_at (BaseEntity 상속 추가)
 * ========================================================= */
ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NULL;


/* =========================================================
 * 3. Alter: payment - add modified_at (BaseEntity 상속 추가)
 * ========================================================= */
ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMP NULL;


/* =========================================================
 * 4. Index: payment_key (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_payment_payment_key
    ON payment(payment_key);


/* =========================================================
 * 5. Index: order_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_payment_order_id
    ON payment(order_id);
