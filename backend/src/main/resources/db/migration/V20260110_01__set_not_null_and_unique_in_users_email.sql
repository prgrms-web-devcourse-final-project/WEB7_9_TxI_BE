CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_not_null
    ON users (email)
    WHERE email IS NOT NULL;