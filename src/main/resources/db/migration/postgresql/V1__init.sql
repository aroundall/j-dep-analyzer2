-- V1: Initial schema for PostgreSQL

CREATE TABLE IF NOT EXISTS artifact (
    gav VARCHAR(500) PRIMARY KEY,
    group_id VARCHAR(255) NOT NULL,
    artifact_id VARCHAR(255) NOT NULL,
    version VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS dependencyedge (
    id BIGSERIAL PRIMARY KEY,
    from_gav VARCHAR(500) NOT NULL,
    to_gav VARCHAR(500) NOT NULL,
    scope VARCHAR(50),
    optional BOOLEAN,
    UNIQUE(from_gav, to_gav, scope, optional)
);

CREATE INDEX IF NOT EXISTS idx_edge_from ON dependencyedge(from_gav);
CREATE INDEX IF NOT EXISTS idx_edge_to ON dependencyedge(to_gav);
CREATE INDEX IF NOT EXISTS idx_artifact_aid ON artifact(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_gid ON artifact(group_id);
