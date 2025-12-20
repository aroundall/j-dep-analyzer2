-- V1: Initial schema for SQLite

CREATE TABLE IF NOT EXISTS artifact (
    gav TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    artifact_id TEXT NOT NULL,
    version TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS dependencyedge (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_gav TEXT NOT NULL,
    to_gav TEXT NOT NULL,
    scope TEXT,
    optional INTEGER,
    UNIQUE(from_gav, to_gav, scope, optional)
);

CREATE INDEX IF NOT EXISTS idx_edge_from ON dependencyedge(from_gav);
CREATE INDEX IF NOT EXISTS idx_edge_to ON dependencyedge(to_gav);
CREATE INDEX IF NOT EXISTS idx_artifact_aid ON artifact(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_gid ON artifact(group_id);
