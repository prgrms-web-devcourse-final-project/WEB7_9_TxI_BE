/* =========================================================
 * 1. Sequence: notification_seq
 * ========================================================= */
CREATE SEQUENCE IF NOT EXISTS notification_seq
    START WITH 1
    INCREMENT BY 1
    CACHE 100;


/* =========================================================
 * 2. Table: notifications (Notification 엔티티 기반)
 * ========================================================= */
CREATE TABLE IF NOT EXISTS notifications
(
    id            BIGINT PRIMARY KEY DEFAULT nextval('notification_seq'),

    type          VARCHAR(255) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    content       TEXT         NOT NULL,

    is_read       BOOLEAN      NOT NULL DEFAULT false,
    read_at       TIMESTAMP    NULL,

    domain_name   VARCHAR(255) NOT NULL,

    user_id       BIGINT       NOT NULL,

    created_at    TIMESTAMP    NULL,
    modified_at   TIMESTAMP    NULL
);


/* =========================================================
 * 3. Foreign Key: notifications.user_id → users.id
 * ========================================================= */
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_notification_user'
    ) THEN
        ALTER TABLE notifications
            ADD CONSTRAINT fk_notification_user
            FOREIGN KEY (user_id)
            REFERENCES users(id);
    END IF;
END $$;


/* =========================================================
 * 4. Index: notifications.user_id (조회 성능)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notifications(user_id);


/* =========================================================
 * 5. Index: notifications.is_read (읽지 않은 알림 조회)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_notifications_is_read
    ON notifications(is_read);


/* =========================================================
 * 6. Index: notifications.created_at (시간순 정렬)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications(created_at DESC);


/* =========================================================
 * 7. Composite Index: user_id + is_read (특정 유저의 읽지 않은 알림 조회)
 * ========================================================= */
CREATE INDEX IF NOT EXISTS idx_notifications_user_is_read
    ON notifications(user_id, is_read)
    WHERE is_read = false;
