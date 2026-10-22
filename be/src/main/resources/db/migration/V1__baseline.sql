-- Baseline: schema as of be/db/db.sql (commit 8225a87).
-- Databases created earlier from db.sql are baselined at this version (spring.flyway.baseline-on-migrate).

CREATE TABLE users (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(255) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	phone_number VARCHAR(20),
    role VARCHAR(25) NOT NULL,
	created_at DATETIME NOT NULL,
	updated_at DATETIME NOT NULL
);

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    family_id CHAR(36) NOT NULL,
    state VARCHAR(25) NOT NULL,
    user_agent VARCHAR(255),
    ip_address VARCHAR(255),
    rotated_at DATETIME,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_users_id_refresh_tokens_id
                            FOREIGN KEY (user_id)
                            REFERENCES users(id),
    INDEX idx_family_id(family_id)
);
