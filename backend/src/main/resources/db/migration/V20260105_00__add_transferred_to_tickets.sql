/* =========================================================
 * tickets - transferred 컬럼 추가 (티켓 양도 기능)
 * ========================================================= */

/* 1) transferred 컬럼 추가 (기본값 false) */
ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS transferred BOOLEAN NOT NULL DEFAULT false;

/* 2) 컬럼 코멘트 */
COMMENT ON COLUMN tickets.transferred IS '양도 여부 (1회만 양도 가능)';
