export interface TranslationResultJSON {
  components: ComponentJSON[];
  totalNodes: number;
  sourceDir: string;
  includeDir: string;
  findings: FindingsJSON[];
}

export interface ComponentJSON {
  name: string;
  translationUnits: TranslationUnitJSON[];
  topLevel?: string;
}

export interface FindingsJSON {
  kind: string;
  path: string;
  component: string;
  rule: string | null;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}

export interface TranslationUnitJSON {
  id: string;
  name: string;
  path: string;
  code: string;
  findings: FindingsJSON[];
}

export interface NodeJSON {
  id: string;
  type: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  code: string;
  name: string;
  astChildren: NodeJSON[];
  prevDFG: EdgeJSON[];
  nextDFG: EdgeJSON[];
}

export interface EdgeJSON {
  id: string;
  label: string;
  start: string;
  end: string;
}
