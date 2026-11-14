-- T58 (P-61): audit log of sensitive actions, written in the same transaction as the change. Append-only; no foreign
-- keys, so the log outlives what it describes. Rows older than housekeeping.retention.audit-events go (T53).
CREATE TABLE audit_events
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at DATETIME(6)  NOT NULL,
    actor_id    BIGINT       NULL,
    actor_email VARCHAR(255) NULL,
    actor_role  VARCHAR(20)  NULL,
    store_id    BIGINT       NULL,
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_id   BIGINT       NULL,
    details     VARCHAR(2000) NULL,
    request_id  VARCHAR(64)  NULL,
    ip          VARCHAR(45)  NULL,
    INDEX idx_audit_events_store_occurred (store_id, occurred_at),
    INDEX idx_audit_events_actor_occurred (actor_id, occurred_at),
    INDEX idx_audit_events_occurred (occurred_at)
);
