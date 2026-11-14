-- T55 (P-60): state of a store for the ADMIN area. READ_ONLY is set by the lock of T56 (P-59); T55 only reads it.
-- lock_reason, locked_at, locked_by describe the current lock and are NULL while ACTIVE.
ALTER TABLE stores
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER timezone,
    ADD COLUMN lock_reason VARCHAR(500) NULL AFTER status,
    ADD COLUMN locked_at DATETIME(6) NULL AFTER lock_reason,
    ADD COLUMN locked_by BIGINT NULL AFTER locked_at,
    ADD CONSTRAINT chk_stores_status CHECK (status IN ('ACTIVE', 'READ_ONLY')),
    ADD CONSTRAINT fk_stores_locked_by FOREIGN KEY (locked_by) REFERENCES users (id),
    ADD INDEX idx_stores_status (status);
