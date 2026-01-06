/* =========================================================
 * v2_orders FK 수정: ticket(ticket_id) → tickets(id)
 * 원본 V20251231_01에서 잘못된 참조가 있었음
 * ========================================================= */

/* 1) 기존 FK 삭제 (있는 경우만) */
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_v2_order_ticket') THEN
        ALTER TABLE v2_orders DROP CONSTRAINT fk_v2_order_ticket;
    END IF;
END $$;

/* 2) 올바른 FK 재생성 */
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_v2_order_ticket') THEN
        ALTER TABLE v2_orders
            ADD CONSTRAINT fk_v2_order_ticket
            FOREIGN KEY (ticket_id) REFERENCES tickets(id);
    END IF;
END $$;
