-- Stores and user tenancy (DATA_MODEL.md §2.1-2.3, T13).
-- No data migration (D-29): the new NOT NULL columns have no default for existing rows, so this fails on a
-- non-empty users table. Reset the database first (DATA_MODEL.md §5).

CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    depot_address VARCHAR(500),
    depot_lat DOUBLE,
    depot_lon DOUBLE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_stores_depot_coordinates
        CHECK ((depot_lat IS NULL AND depot_lon IS NULL) OR (depot_lat IS NOT NULL AND depot_lon IS NOT NULL))
);

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(100) AFTER password,
    ADD COLUMN status VARCHAR(30) NOT NULL AFTER role,
    ADD COLUMN store_id BIGINT AFTER status,
    ADD COLUMN email_verified_at DATETIME(6) AFTER store_id,
    ADD COLUMN can_drive BOOLEAN NOT NULL DEFAULT FALSE AFTER email_verified_at,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL,
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_users_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    ADD CONSTRAINT chk_users_store_by_role
        CHECK ((role = 'ADMIN' AND store_id IS NULL) OR (role <> 'ADMIN' AND store_id IS NOT NULL)),
    ADD INDEX idx_users_store_role (store_id, role);

ALTER TABLE refresh_tokens
    MODIFY COLUMN rotated_at DATETIME(6),
    MODIFY COLUMN expires_at DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL,
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL,
    ADD INDEX idx_refresh_tokens_user_state (user_id, state);
