-- Store invitations (DATA_MODEL.md §2.4, T16).

CREATE TABLE store_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(25) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by BIGINT NOT NULL,
    accepted_user_id BIGINT,
    expires_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_store_invitations_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_store_invitations_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT fk_store_invitations_accepted_user_id FOREIGN KEY (accepted_user_id) REFERENCES users (id),
    CONSTRAINT chk_store_invitations_role CHECK (role IN ('DISPATCHER', 'DRIVER')),
    INDEX idx_store_invitations_store_status (store_id, status),
    INDEX idx_store_invitations_email_status (email, status)
);
