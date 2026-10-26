-- Creates the first platform ADMIN (DATA_MODEL.md §5). Not run automatically.
-- 1. Copy this file outside the repository and replace the email and the password hash.
--    Generate a BCrypt hash, for example: htpasswd -bnBC 10 "" 'your-password' | tr -d ':\n' | sed 's/^\$2y/$2a/'
-- 2. Run it against the database after Flyway has migrated it, e.g.:
--    docker compose exec -T mariadb sh -c 'mariadb -u root -p"$MARIADB_ROOT_PASSWORD" "$MARIADB_DATABASE"' < seed-admin.sql
-- ADMIN has no store (store_id NULL) and an already verified email.

INSERT INTO users (email, password, full_name, role, status, store_id, email_verified_at, can_drive, created_at, updated_at)
VALUES ('admin@example.com',
        '$2a$10$REPLACE_WITH_A_REAL_BCRYPT_HASH_OF_60_CHARACTERS_xxxxxxxx',
        'Admin',
        'ADMIN',
        'ACTIVE',
        NULL,
        UTC_TIMESTAMP(6),
        FALSE,
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6));
