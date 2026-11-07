ALTER TABLE scan_dependencies
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'ANALYZED';

UPDATE scan_dependencies SET status = 'SKIPPED_UNRESOLVED_VERSION'
WHERE version IS NULL OR btrim(version) = '' OR version LIKE ('%' || chr(36) || '{%');

ALTER TABLE scan_dependencies ADD CONSTRAINT ck_dependency_status
    CHECK (status IN ('ANALYZED', 'SKIPPED_UNRESOLVED_VERSION'));

CREATE INDEX idx_scan_dependencies_scan_id ON scan_dependencies(scan_id);
