CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
    ON users (email);

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;