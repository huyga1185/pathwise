-- Vehicles of a store (DATA_MODEL.md §2.5, T20). Never hard-deleted once used in a plan: set active = FALSE instead.

CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    plate_number VARCHAR(20),
    capacity INT NOT NULL,
    -- D-36: KG, BOX, PACKAGE, ORDER (enum CapacityUnit); no CHECK on the list, so a new unit needs no migration.
    capacity_unit VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_vehicles_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT uk_vehicles_store_name UNIQUE (store_id, name),
    CONSTRAINT chk_vehicles_capacity CHECK (capacity > 0)
);
