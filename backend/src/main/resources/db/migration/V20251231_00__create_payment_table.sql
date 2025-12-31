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
 * 2. Index: payment_key (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_payment_payment_key
    ON payment(payment_key);


/* =========================================================
 * 3. Index: order_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_payment_order_id
    ON payment(order_id);
