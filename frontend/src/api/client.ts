import type {
    Artifact,
    DependencyRow,
    UploadResult,
    GraphData,
    TableInfo,
    GraphParams,
    DependencyParams,
} from '../types/api';

const API_BASE = '/api';

// Helper to build query string
function buildQuery(params: Record<string, unknown>): string {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value === undefined || value === null) return;
        if (Array.isArray(value)) {
            value.forEach((v) => searchParams.append(key, String(v)));
        } else {
            searchParams.append(key, String(value));
        }
    });
    return searchParams.toString();
}

// Upload POM files
export async function uploadPoms(files: FileList): Promise<UploadResult> {
    const formData = new FormData();
    Array.from(files).forEach((file) => formData.append('files', file));

    const res = await fetch(`${API_BASE}/upload`, {
        method: 'POST',
        body: formData,
    });
    return res.json();
}

// Get all artifacts
export async function getArtifacts(limit = 500): Promise<Artifact[]> {
    const res = await fetch(`${API_BASE}/artifacts?limit=${limit}`);
    return res.json();
}

// Get graph data in Cytoscape format
export async function getGraphData(params: GraphParams): Promise<GraphData> {
    const query = buildQuery({
        root_id: params.rootId,
        direction: params.direction ?? 'forward',
        show_group: params.showGroup ?? true,
        show_version: params.showVersion ?? true,
        depth: params.depth,
        scope: params.scope,
    });
    const res = await fetch(`${API_BASE}/graph/data?${query}`);
    return res.json();
}

// Get dependencies table data
export async function getDependencies(params: DependencyParams): Promise<DependencyRow[]> {
    const query = buildQuery({
        q: params.q,
        group_q: params.groupQ,
        scope: params.scope,
        ignore_version: params.ignoreVersion ?? false,
        ignore_group: params.ignoreGroup ?? false,
        limit: params.limit ?? 500,
    });
    const res = await fetch(`${API_BASE}/dependencies/table?${query}`);
    return res.json();
}

// Get available scopes (extract from unique dependencies)
export async function getScopes(): Promise<string[]> {
    // For now, get all dependencies and extract unique scopes
    // In production, you'd want a dedicated API endpoint
    const deps = await getDependencies({ limit: 1000 });
    const scopes = new Set(deps.map((d) => d.scope).filter(Boolean));
    return Array.from(scopes);
}

// Get export table info
export async function getExportTables(): Promise<TableInfo[]> {
    // Note: This info is not available via API, so we'll fetch counts separately
    const [artifacts, deps] = await Promise.all([
        getArtifacts(1),
        getDependencies({ limit: 1 }),
    ]);

    // The actual counts require backend changes, for now return placeholder
    return [
        { name: 'artifact', rowCount: artifacts.length > 0 ? -1 : 0 },
        { name: 'dependencyedge', rowCount: deps.length > 0 ? -1 : 0 },
    ];
}

// Get CSV export URL
export function getExportUrl(table: string): string {
    return `${API_BASE}/export/${table}.csv`;
}

// Get filtered dependencies CSV export URL
export function getDependenciesExportUrl(params: DependencyParams): string {
    const query = buildQuery({
        q: params.q,
        group_q: params.groupQ,
        scope: params.scope,
        ignore_version: params.ignoreVersion,
        ignore_group: params.ignoreGroup,
    });
    return `${API_BASE}/dependencies/export?${query}`;
}
