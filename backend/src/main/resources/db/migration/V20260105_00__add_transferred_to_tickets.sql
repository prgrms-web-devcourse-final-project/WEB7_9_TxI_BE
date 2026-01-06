/* =========================================================
 * tickets - transferred 컬럼 추가 (티켓 양도 기능)
 * ========================================================= */

/* 1) transferred 컬럼 추가 (기본값 false) */
ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS transferred BOOLEAN NOT NULL DEFAULT false;

/* 2) 컬럼 코멘트 */
COMMENT ON COLUMN tickets.transferred IS '양도 여부 (1회만 양도 가능)';


/* =========================================================
 * ticket_transfer_history - 양도 이력 테이블
 * ========================================================= */

/* 3) 양도 이력 테이블 생성 */
CREATE TABLE IF NOT EXISTS ticket_transfer_history (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    from_user_id    BIGINT NOT NULL,
    to_user_id      BIGINT NOT NULL,
    transferred_at  TIMESTAMP NOT NULL,
    created_at      TIMESTAMP,
    modified_at     TIMESTAMP
);

/* 4) 인덱스 */
CREATE INDEX IF NOT EXISTS idx_transfer_history_ticket_id
    ON ticket_transfer_history (ticket_id);

/* 5) 코멘트 */
COMMENT ON TABLE ticket_transfer_history IS '티켓 양도 이력';
COMMENT ON COLUMN ticket_transfer_history.ticket_id IS '양도된 티켓 ID';
COMMENT ON COLUMN ticket_transfer_history.from_user_id IS '양도한 사용자 ID';
COMMENT ON COLUMN ticket_transfer_history.to_user_id IS '양도받은 사용자 ID';
COMMENT ON COLUMN ticket_transfer_history.transferred_at IS '양도 시각';
