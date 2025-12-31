/* =========================================================
 * 1. Drop FK: fk_v2_order_ticket (잘못된 참조 제거)
 * ========================================================= */
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_v2_order_ticket'
    ) THEN
        ALTER TABLE v2_orders
            DROP CONSTRAINT fk_v2_order_ticket;
    END IF;
END $$;


/* =========================================================
 * 2. Foreign Key: v2_orders.ticket_id → tickets.id (올바른 참조)
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
            REFERENCES tickets(id);
    END IF;
END $$;
