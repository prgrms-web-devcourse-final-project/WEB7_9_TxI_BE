/* =========================================================
 * 1. Table: v2_orders
 * ========================================================= */
CREATE TABLE IF NOT EXISTS v2_orders (
    v2_order_id     VARCHAR(36)  PRIMARY KEY,
    ticket_id       BIGINT       NOT NULL,
    amount          BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_key     VARCHAR(255) NULL,
    payment_id      BIGINT       NULL,

    created_at      TIMESTAMP NULL,
    modified_at     TIMESTAMP NULL
);


/* =========================================================
 * 2. Alter: v2_orders - add payment_id (신규 컬럼 추가)
 * ========================================================= */
ALTER TABLE v2_orders
    ADD COLUMN IF NOT EXISTS payment_id BIGINT NULL;


/* =========================================================
 * 3. Foreign Key: v2_orders.ticket_id → ticket.ticket_id
 * ========================================================= */
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_v2_order_ticket'
    ) THEN
        ALTER TABLE v2_orders
            ADD CONSTRAINT fk_v2_order_ticket
            FOREIGN KEY (ticket_id)
            REFERENCES ticket(ticket_id);
    END IF;
END $$;


/* =========================================================
 * 3. Foreign Key: v2_orders.payment_id → payment.payment_id
 * ========================================================= */
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_v2_order_payment'
    ) THEN
        ALTER TABLE v2_orders
            ADD CONSTRAINT fk_v2_order_payment
            FOREIGN KEY (payment_id)
            REFERENCES payment(payment_id);
    END IF;
END $$;


/* =========================================================
 * 4. Index: v2_orders.ticket_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_v2_orders_ticket_id
    ON v2_orders(ticket_id);


/* =========================================================
 * 5. Index: v2_orders.payment_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_v2_orders_payment_id
    ON v2_orders(payment_id);


/* =========================================================
 * 6. Index: v2_orders.payment_key (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_v2_orders_payment_key
    ON v2_orders(payment_key);
