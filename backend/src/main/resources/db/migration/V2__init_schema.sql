/* =========================================================
 * V1: 전체 스키마 초기화
 * - 시퀀스, 기본 테이블, FK, 인덱스 전부 포함
 * ========================================================= */

/* ===========================================
 * 1. SEQUENCES
 * =========================================== */
CREATE SEQUENCE IF NOT EXISTS store_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS user_seq START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE IF NOT EXISTS seat_seq START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE IF NOT EXISTS ticket_seq START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE IF NOT EXISTS pre_register_seq START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE IF NOT EXISTS queue_entry_seq START WITH 1 INCREMENT BY 100;

/* ===========================================
 * 2. stores
 * =========================================== */
CREATE TABLE IF NOT EXISTS stores (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('store_seq'),
    name                VARCHAR(30) NOT NULL,
    registration_number VARCHAR(16) NOT NULL,
    address             VARCHAR(255) NOT NULL,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP,
    modified_at         TIMESTAMP
);

/* ===========================================
 * 3. users
 * =========================================== */
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT PRIMARY KEY DEFAULT nextval('user_seq'),
    email           VARCHAR(100),
    full_name       VARCHAR(30) NOT NULL,
    nickname        VARCHAR(20) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    birth_date      DATE,
    role            VARCHAR(20) NOT NULL,
    deleted_status  VARCHAR(20) NOT NULL,
    deleted_at      TIMESTAMP,
    provider_type   VARCHAR(20),
    provider_id     VARCHAR(255),
    store_id        BIGINT,
    created_at      TIMESTAMP,
    modified_at     TIMESTAMP
);

/* ===========================================
 * 4. events
 * =========================================== */
CREATE TABLE IF NOT EXISTS events (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    category          VARCHAR(50) NOT NULL,
    description       TEXT,
    place             VARCHAR(255) NOT NULL,
    image_url         VARCHAR(500),
    min_price         INTEGER NOT NULL,
    max_price         INTEGER NOT NULL,
    pre_open_at       TIMESTAMP NOT NULL,
    pre_close_at      TIMESTAMP NOT NULL,
    ticket_open_at    TIMESTAMP NOT NULL,
    ticket_close_at   TIMESTAMP NOT NULL,
    event_date        TIMESTAMP NOT NULL,
    max_ticket_amount INTEGER NOT NULL,
    status            VARCHAR(20) NOT NULL,
    deleted           BOOLEAN NOT NULL DEFAULT false,
    store_id          BIGINT NOT NULL,
    created_at        TIMESTAMP,
    modified_at       TIMESTAMP
);

/* ===========================================
 * 5. seats
 * =========================================== */
CREATE TABLE IF NOT EXISTS seats (
    id          BIGINT PRIMARY KEY DEFAULT nextval('seat_seq'),
    event_id    BIGINT NOT NULL,
    seat_code   VARCHAR(20) NOT NULL,
    grade       VARCHAR(20) NOT NULL,
    price       INTEGER NOT NULL,
    seat_status VARCHAR(20) NOT NULL,
    version     INTEGER DEFAULT 0,
    created_at  TIMESTAMP,
    modified_at TIMESTAMP
);

/* ===========================================
 * 6. tickets
 * =========================================== */
CREATE TABLE IF NOT EXISTS tickets (
    id            BIGINT PRIMARY KEY DEFAULT nextval('ticket_seq'),
    owner_user_id BIGINT NOT NULL,
    seat_id       BIGINT,
    event_id      BIGINT NOT NULL,
    ticket_status VARCHAR(20) NOT NULL,
    issued_at     TIMESTAMP,
    used_at       TIMESTAMP,
    transferred   BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMP,
    modified_at   TIMESTAMP
);

/* ===========================================
 * 7. ticket_transfer_history
 * =========================================== */
CREATE TABLE IF NOT EXISTS ticket_transfer_history (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    from_user_id    BIGINT NOT NULL,
    to_user_id      BIGINT NOT NULL,
    transferred_at  TIMESTAMP NOT NULL,
    created_at      TIMESTAMP,
    modified_at     TIMESTAMP
);

/* ===========================================
 * 8. orders (legacy Order entity)
 * =========================================== */
CREATE TABLE IF NOT EXISTS orders (
    id           BIGSERIAL PRIMARY KEY,
    ticket_id    BIGINT NOT NULL,
    amount       BIGINT,
    status       VARCHAR(20),
    payment_key  VARCHAR(255),
    order_key    VARCHAR(255),
    order_number VARCHAR(50),
    paid_at      TIMESTAMP,
    created_at   TIMESTAMP,
    modified_at  TIMESTAMP
);

/* ===========================================
 * 9. payments
 * =========================================== */
CREATE TABLE IF NOT EXISTS payments (
    payment_id  BIGSERIAL PRIMARY KEY,
    payment_key VARCHAR(255) NOT NULL,
    order_id    VARCHAR(255) NOT NULL,
    amount      BIGINT NOT NULL,
    method      VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP,
    modified_at TIMESTAMP
);

/* ===========================================
 * 10. v2_orders
 * =========================================== */
CREATE TABLE IF NOT EXISTS v2_orders (
    v2_order_id VARCHAR(36) PRIMARY KEY,
    ticket_id   BIGINT NOT NULL,
    amount      BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_key VARCHAR(255),
    payment_id  BIGINT,
    created_at  TIMESTAMP,
    modified_at TIMESTAMP
);

/* ===========================================
 * 11. pre_registers
 * =========================================== */
CREATE TABLE IF NOT EXISTS pre_registers (
    id                        BIGINT PRIMARY KEY DEFAULT nextval('pre_register_seq'),
    pre_register_status       VARCHAR(20) NOT NULL,
    event_id                  BIGINT NOT NULL,
    user_id                   BIGINT NOT NULL,
    pre_register_agree_terms  BOOLEAN NOT NULL,
    pre_register_agree_privacy BOOLEAN NOT NULL,
    pre_register_agreed_at    TIMESTAMP NOT NULL,
    created_at                TIMESTAMP,
    modified_at               TIMESTAMP
);

/* ===========================================
 * 12. queue_entries
 * =========================================== */
CREATE TABLE IF NOT EXISTS queue_entries (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('queue_entry_seq'),
    queue_rank          INTEGER NOT NULL,
    queue_entry_status  VARCHAR(20) NOT NULL,
    entered_at          TIMESTAMP,
    expired_at          TIMESTAMP,
    user_id             BIGINT NOT NULL,
    event_id            BIGINT NOT NULL,
    created_at          TIMESTAMP,
    modified_at         TIMESTAMP
);

/* ===========================================
 * 13. refresh_tokens
 * =========================================== */
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    token         VARCHAR(512) NOT NULL,
    jti           VARCHAR(36),
    issued_at     TIMESTAMP,
    expires_at    TIMESTAMP,
    session_id    VARCHAR(36),
    token_version BIGINT,
    revoked       BOOLEAN DEFAULT false,
    user_agent    VARCHAR(500),
    ip_address    VARCHAR(50),
    created_at    TIMESTAMP,
    modified_at   TIMESTAMP
);

/* ===========================================
 * 14. active_sessions
 * =========================================== */
CREATE TABLE IF NOT EXISTS active_sessions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    session_id    VARCHAR(36) NOT NULL,
    token_version BIGINT NOT NULL,
    last_login_at TIMESTAMP NOT NULL,
    created_at    TIMESTAMP,
    modified_at   TIMESTAMP
);

/* ===========================================
 * 15. notifications
 * =========================================== */
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(50) NOT NULL,
    type_detail VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT false,
    read_at     TIMESTAMP,
    domain_name VARCHAR(50) NOT NULL,
    domain_id   BIGINT,
    user_id     BIGINT NOT NULL,
    created_at  TIMESTAMP,
    modified_at TIMESTAMP
);

/* ===========================================
 * 16. FOREIGN KEYS
 * =========================================== */

-- users -> stores
ALTER TABLE users
    ADD CONSTRAINT fk_user_store
    FOREIGN KEY (store_id) REFERENCES stores(id);

-- events -> stores
ALTER TABLE events
    ADD CONSTRAINT fk_events_store
    FOREIGN KEY (store_id) REFERENCES stores(id);

-- seats -> events
ALTER TABLE seats
    ADD CONSTRAINT fk_seat_event
    FOREIGN KEY (event_id) REFERENCES events(id);

-- tickets -> users, seats, events
ALTER TABLE tickets
    ADD CONSTRAINT fk_ticket_owner
    FOREIGN KEY (owner_user_id) REFERENCES users(id);

ALTER TABLE tickets
    ADD CONSTRAINT fk_ticket_seat
    FOREIGN KEY (seat_id) REFERENCES seats(id);

ALTER TABLE tickets
    ADD CONSTRAINT fk_ticket_event
    FOREIGN KEY (event_id) REFERENCES events(id);

-- orders -> tickets
ALTER TABLE orders
    ADD CONSTRAINT fk_order_ticket
    FOREIGN KEY (ticket_id) REFERENCES tickets(id);

-- v2_orders -> tickets, payments
ALTER TABLE v2_orders
    ADD CONSTRAINT fk_v2_order_ticket
    FOREIGN KEY (ticket_id) REFERENCES tickets(id);

ALTER TABLE v2_orders
    ADD CONSTRAINT fk_v2_order_payment
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id);

-- pre_registers -> events, users
ALTER TABLE pre_registers
    ADD CONSTRAINT fk_pre_register_event
    FOREIGN KEY (event_id) REFERENCES events(id);

ALTER TABLE pre_registers
    ADD CONSTRAINT fk_pre_register_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- queue_entries -> users, events
ALTER TABLE queue_entries
    ADD CONSTRAINT fk_queue_entry_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE queue_entries
    ADD CONSTRAINT fk_queue_entry_event
    FOREIGN KEY (event_id) REFERENCES events(id);

-- refresh_tokens -> users
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- active_sessions -> users
ALTER TABLE active_sessions
    ADD CONSTRAINT fk_active_session_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- notifications -> users
ALTER TABLE notifications
    ADD CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id) REFERENCES users(id);

/* ===========================================
 * 17. UNIQUE CONSTRAINTS
 * =========================================== */

-- seats: 이벤트별 등급+좌석코드 유니크
ALTER TABLE seats
    ADD CONSTRAINT uk_event_grade_seatcode
    UNIQUE (event_id, grade, seat_code);

-- pre_registers: 이벤트+유저 유니크
ALTER TABLE pre_registers
    ADD CONSTRAINT uk_pre_register_event_user
    UNIQUE (event_id, user_id);

-- active_sessions: 유저당 하나의 세션
ALTER TABLE active_sessions
    ADD CONSTRAINT uk_active_session_user
    UNIQUE (user_id);

/* ===========================================
 * 18. INDEXES
 * =========================================== */

-- users
CREATE INDEX IF NOT EXISTS idx_users_store_id ON users(store_id);

-- events
CREATE INDEX IF NOT EXISTS idx_events_store_id ON events(store_id);

-- seats
CREATE INDEX IF NOT EXISTS idx_seats_event_id ON seats(event_id);

-- tickets
CREATE INDEX IF NOT EXISTS idx_tickets_owner ON tickets(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_event ON tickets(event_id);
CREATE INDEX IF NOT EXISTS idx_tickets_seat ON tickets(seat_id);

-- ticket_transfer_history
CREATE INDEX IF NOT EXISTS idx_transfer_history_ticket_id ON ticket_transfer_history(ticket_id);

-- orders
CREATE INDEX IF NOT EXISTS idx_orders_ticket_id ON orders(ticket_id);

-- v2_orders
CREATE INDEX IF NOT EXISTS idx_v2_orders_ticket_id ON v2_orders(ticket_id);
CREATE INDEX IF NOT EXISTS idx_v2_orders_payment_id ON v2_orders(payment_id);
CREATE INDEX IF NOT EXISTS idx_v2_orders_payment_key ON v2_orders(payment_key);

-- pre_registers
CREATE INDEX IF NOT EXISTS idx_pre_registers_event ON pre_registers(event_id);
CREATE INDEX IF NOT EXISTS idx_pre_registers_user ON pre_registers(user_id);

-- queue_entries
CREATE INDEX IF NOT EXISTS idx_queue_entries_user ON queue_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_queue_entries_event ON queue_entries(event_id);

-- refresh_tokens
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_revoked ON refresh_tokens(user_id, revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_expires_at ON refresh_tokens(user_id, expires_at);

-- active_sessions
CREATE INDEX IF NOT EXISTS idx_active_session_session_id ON active_sessions(session_id);

-- notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
