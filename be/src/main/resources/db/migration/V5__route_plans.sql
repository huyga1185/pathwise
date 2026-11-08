-- Route plans, their routes, stops and stop demands, and the shared background job table (DATA_MODEL.md §2.6-§2.9, T24).

CREATE TABLE background_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    -- The plan id; no FK so that the table stays generic (D-19).
    target_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    attempts INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    started_at DATETIME(6),
    finished_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_background_jobs_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_background_jobs_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_background_jobs_status_created (status, created_at),
    INDEX idx_background_jobs_store_target (store_id, target_id)
);

CREATE TABLE route_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    plan_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    -- Snapshot of the store depot, or the depot given for this plan (D-20).
    depot_address VARCHAR(500),
    depot_lat DOUBLE NOT NULL,
    depot_lon DOUBLE NOT NULL,
    timezone VARCHAR(50) NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    active_job_id BIGINT,
    -- NULL: no valid optimization result.
    optimized_at DATETIME(6),
    total_distance_m INT,
    total_duration_s INT,
    created_by BIGINT NOT NULL,
    published_at DATETIME(6),
    completed_at DATETIME(6),
    cancelled_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_route_plans_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_route_plans_active_job_id FOREIGN KEY (active_job_id) REFERENCES background_jobs (id),
    CONSTRAINT fk_route_plans_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_route_plans_store_date (store_id, plan_date),
    INDEX idx_route_plans_store_status (store_id, status)
);

-- One assignment (driver + vehicle + shift) and, after optimization, its result.
CREATE TABLE route_plan_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    -- Snapshot when the assignment is saved (D-36).
    vehicle_capacity INT NOT NULL,
    vehicle_capacity_unit VARCHAR(20) NOT NULL,
    shift_start TIME NOT NULL,
    shift_end TIME NOT NULL,
    max_distance_m INT,
    -- Start and end of this route; the plan depot in the MVP, the extension point for multi-depot (D-20).
    start_address VARCHAR(500),
    end_address VARCHAR(500),
    start_lat DOUBLE NOT NULL,
    start_lon DOUBLE NOT NULL,
    end_lat DOUBLE NOT NULL,
    end_lon DOUBLE NOT NULL,
    planned_distance_m INT,
    planned_duration_s INT,
    planned_start DATETIME(6),
    planned_end DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_route_plan_routes_plan_id FOREIGN KEY (plan_id) REFERENCES route_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_plan_routes_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_route_plan_routes_driver_id FOREIGN KEY (driver_id) REFERENCES users (id),
    CONSTRAINT fk_route_plan_routes_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT uk_route_plan_routes_plan_driver UNIQUE (plan_id, driver_id),
    CONSTRAINT uk_route_plan_routes_plan_vehicle UNIQUE (plan_id, vehicle_id),
    CONSTRAINT chk_route_plan_routes_shift CHECK (shift_start < shift_end),
    CONSTRAINT chk_route_plan_routes_max_distance CHECK (max_distance_m IS NULL OR max_distance_m > 0),
    CONSTRAINT chk_route_plan_routes_capacity CHECK (vehicle_capacity > 0),
    INDEX idx_route_plan_routes_driver (driver_id)
);

CREATE TABLE route_plan_stops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    -- NULL: not assigned yet, or dropped by the solver (P-13).
    route_id BIGINT,
    sequence INT,
    label VARCHAR(150) NOT NULL,
    recipient_name VARCHAR(100),
    recipient_phone VARCHAR(20),
    address_text VARCHAR(500),
    lat DOUBLE,
    lon DOUBLE,
    geocode_status VARCHAR(20) NOT NULL,
    geocode_error VARCHAR(50),
    geocoded_display_name VARCHAR(500),
    service_time_s INT NOT NULL DEFAULT 0,
    tw_start TIME,
    tw_end TIME,
    note VARCHAR(500),
    planned_arrival DATETIME(6),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(255),
    status_updated_at DATETIME(6),
    status_updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_route_plan_stops_plan_id FOREIGN KEY (plan_id) REFERENCES route_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_plan_stops_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_route_plan_stops_route_id FOREIGN KEY (route_id) REFERENCES route_plan_routes (id) ON DELETE SET NULL,
    CONSTRAINT fk_route_plan_stops_status_updated_by FOREIGN KEY (status_updated_by) REFERENCES users (id),
    CONSTRAINT chk_route_plan_stops_location CHECK (address_text IS NOT NULL OR (lat IS NOT NULL AND lon IS NOT NULL)),
    CONSTRAINT chk_route_plan_stops_coordinates CHECK ((lat IS NULL) = (lon IS NULL)),
    CONSTRAINT chk_route_plan_stops_service_time CHECK (service_time_s >= 0),
    CONSTRAINT chk_route_plan_stops_time_window CHECK ((tw_start IS NULL AND tw_end IS NULL)
        OR (tw_start IS NOT NULL AND tw_end IS NOT NULL AND tw_start < tw_end)),
    INDEX idx_route_plan_stops_plan (plan_id),
    INDEX idx_route_plan_stops_route_sequence (route_id, sequence)
);

-- Demand of a stop per capacity unit (D-36). No row for a unit means 0; ORDER is never stored (each stop counts 1).
CREATE TABLE route_plan_stop_demands (
    stop_id BIGINT NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT pk_route_plan_stop_demands PRIMARY KEY (stop_id, unit),
    CONSTRAINT fk_route_plan_stop_demands_stop_id FOREIGN KEY (stop_id) REFERENCES route_plan_stops (id) ON DELETE CASCADE,
    CONSTRAINT chk_route_plan_stop_demands_quantity CHECK (quantity >= 0)
);
