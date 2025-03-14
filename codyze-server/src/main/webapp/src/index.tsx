export interface TranslationResult {
    components: Component[];
    totalNodes: number;
}

export interface Component {
    name: string;
    translationUnits: TranslationUnit[];
}

export interface TranslationUnit {
    name: string;
    path: string;
    code: string;
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