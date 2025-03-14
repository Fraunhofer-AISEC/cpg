// src/types.ts
export interface TranslationResult {
    components: Component[];
    totalNodes: number;
    sourceDir: string
    includeDir: string
}

export interface Component {
    name: string;
    translationUnits: TranslationUnit[];
    topLevel?: string
}

export interface FindingsJSON {
    kind: string;
    path: string;
    rule: string | null;
    startLine: number;
    startColumn: number;
    endLine: number;
    endColumn: number;
}

export interface TranslationUnit {
    name: string;
    path: string;
    code: string;
    astNodes: NodeInfo[];
    overlayNodes: NodeInfo[];
    findings: FindingsJSON[];
}

export interface NodeInfo {
    id: string;
    type: string;
    startLine: number;
    startColumn: number;
    endLine: number;
    endColumn: number;
    code: string;
    name: string;
}