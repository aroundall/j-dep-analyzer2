// API response types matching Spring Boot backend

export interface Artifact {
    id: number;
    gav: string;
    groupId: string;
    artifactId: string;
    version: string;
}

export interface DependencyRow {
    fromGav: string;
    fromGroup: string;
    fromArtifact: string;
    fromVersion: string;
    toGav: string;
    toGroup: string;
    toArtifact: string;
    toVersion: string;
    scope: string;
}

export interface UploadResult {
    success: boolean;
    parsed: number;
    newArtifacts: number;
    newEdges: number;
    skipped: number;
    errors?: string[];
}

export interface GraphData {
    elements: CytoscapeElement[];
}

export interface CytoscapeElement {
    data: {
        id: string;
        label?: string;
        source?: string;
        target?: string;
        [key: string]: unknown;
    };
    classes?: string;
}

export interface TableInfo {
    name: string;
    rowCount: number;
}

export interface GraphParams {
    rootId?: string;
    direction?: 'forward' | 'reverse';
    showGroup?: boolean;
    showVersion?: boolean;
    depth?: number;
    scope?: string[];
}

export interface DependencyParams {
    q?: string;
    groupQ?: string;
    scope?: string[];
    ignoreVersion?: boolean;
    ignoreGroup?: boolean;
    limit?: number;
}
