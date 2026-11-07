CREATE TABLE scans (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scan_dependencies (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    artifact_id VARCHAR(255) NOT NULL,
    version VARCHAR(255),
    vulnerabilities JSONB NOT NULL DEFAULT '[]'::jsonb,

    CONSTRAINT fk_scan_dependencies_scan
        FOREIGN KEY (scan_id)
        REFERENCES scans(id)
        ON DELETE CASCADE
);