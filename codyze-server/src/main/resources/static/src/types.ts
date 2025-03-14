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

export interface TranslationUnit {
    name: string;
    path: string;
    code: string;
    astNodes: NodeInfo[];
    overlayNodes: NodeInfo[];
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