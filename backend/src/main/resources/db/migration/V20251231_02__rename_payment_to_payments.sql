/* =========================================================
 * 1. Rename Table: payment → payments
 *    - Payment 엔티티의 @Table(name="payments")와 일치하도록 테이블명 변경
 *    - FK 제약 조건은 자동으로 업데이트됨
 * ========================================================= */
ALTER TABLE payment RENAME TO payments;
