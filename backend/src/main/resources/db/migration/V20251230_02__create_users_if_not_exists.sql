/* =========================================================
 * 0-1. Sequence: user_seq
 * ========================================================= */
CREATE SEQUENCE IF NOT EXISTS user_seq
    START WITH 1
    INCREMENT BY 1
    CACHE 100;

/* =========================================================
 * 0-2. Table: users (User 엔티티 기반)
 *  - store_id 는 뒤에서 ALTER/ FK 걸어도 되지만,
 *    "users 자체가 없는 문제"를 막기 위해 여기서 같이 만들어도 됨
 * ========================================================= */
CREATE TABLE IF NOT EXISTS users
(
    id             BIGINT PRIMARY KEY DEFAULT nextval('user_seq'),

    email          VARCHAR(100) NOT NULL,
    full_name      VARCHAR(30)  NOT NULL,
    nickname       VARCHAR(20)  NOT NULL,
    password       TEXT         NOT NULL,
    birth_date     DATE         NULL,

    role           VARCHAR(255) NOT NULL,
    deleted_status VARCHAR(255) NOT NULL,

    deleted_at     TIMESTAMP    NULL,

    store_id       BIGINT       NULL,

    created_at     TIMESTAMP    NULL,
    modified_at    TIMESTAMP    NULL
);

/* =========================================================
 * 0-3. Unique constraints (엔티티의 unique=true 반영)
 * ========================================================= */
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email
    ON users (email);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_nickname
    ON users (nickname);

/* =========================================================
 * 0-4. (선택) deleted_at IS NULL 조건으로만 유니크를 원하면
 *  - 현재 엔티티는 "그냥 unique"라서 논리삭제 후에도 중복 불가.
 *  - 만약 논리삭제 후 재가입/재사용을 허용하려면 위 uk_* 대신 이걸 사용.
 * ========================================================= */
/*
DROP INDEX IF EXISTS uk_users_email;
DROP INDEX IF EXISTS uk_users_nickname;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_active
    ON users(email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_nickname_active
    ON users(nickname)
    WHERE deleted_at IS NULL;
*/