export interface AnalysisResultJSON {
  components: ComponentJSON[];
  totalNodes: number;
  sourceDir: string;
  includeDir: string;
  findings: FindingsJSON[];
  requirementCategories: RequirementsCategoryJSON[];
}

export interface AnalysisProjectJSON {
  name: string;
  sourceDir: string;
  includeDir: string | null;
  topLevel: string | null;
  projectCreatedAt: string;
  lastAnalyzedAt: string | null;
  requirementCategories: RequirementsCategoryJSON[];
}

export interface ComponentJSON {
  name: string;
  translationUnits: TranslationUnitJSON[];
  topLevel?: string;
}

export interface FindingsJSON {
  kind: string;
  path: string;
  component: string | null;
  translationUnit: string | null;
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

export interface ConceptInfo {
  conceptName: string;
  constructorInfo: ConstructorInfo[];
}

export interface RequirementsCategoryJSON {
  id: string;
  name: string;
  description: string;
  requirements: RequirementJSON[];
}

export interface RequirementJSON {
  id: string;
  name: string;
  description: string;
  status: string;
  categoryId: string;
  queryTree?: QueryTreeJSON;
}

export interface CallerInfoJSON {
  className: string;
  methodName: string;
  fileName: string;
  lineNumber: number;
}

export interface QueryTreeJSON {
  id: string;
  value: string;
  confidence: string;
  stringRepresentation: string;
  operator: string;
  queryTreeType: string;
  childrenIds: string[];
  hasChildren: boolean;
  nodeId?: string;
  callerInfo?: CallerInfoJSON;
}

export interface ConstructorInfo {
  argumentName: string;
  argumentType: string;
  isOptional: boolean;
}

// QueryTree status determination and styling
export type QueryTreeStatus =
  | 'FULFILLED'
  | 'VIOLATED'
  | 'REJECTED'
  | 'UNDECIDED'
  | 'NOT_YET_EVALUATED'
  | 'NON_BOOLEAN';

export interface QueryTreeStatusConfig {
  bgColor: string;
  textColor: string;
  badgeColor: string;
  borderColor: string;
  icon: string;
}

export const queryTreeStatusConfigs: Record<QueryTreeStatus, QueryTreeStatusConfig> = {
  FULFILLED: {
    bgColor: 'bg-green-50',
    textColor: 'text-green-700',
    badgeColor: 'bg-green-100 text-green-800',
    borderColor: 'border-green-200',
    icon: '✓'
  },
  VIOLATED: {
    bgColor: 'bg-red-50',
    textColor: 'text-red-700',
    badgeColor: 'bg-red-100 text-red-800',
    borderColor: 'border-red-200',
    icon: '✕'
  },
  REJECTED: {
    bgColor: 'bg-orange-50',
    textColor: 'text-orange-700',
    badgeColor: 'bg-orange-100 text-orange-800',
    borderColor: 'border-orange-200',
    icon: '⚠'
  },
  UNDECIDED: {
    bgColor: 'bg-yellow-50',
    textColor: 'text-yellow-700',
    badgeColor: 'bg-yellow-100 text-yellow-800',
    borderColor: 'border-yellow-200',
    icon: '?'
  },
  NOT_YET_EVALUATED: {
    bgColor: 'bg-gray-50',
    textColor: 'text-gray-700',
    badgeColor: 'bg-gray-100 text-gray-800',
    borderColor: 'border-gray-200',
    icon: '⏳'
  },
  NON_BOOLEAN: {
    bgColor: 'bg-gray-50',
    textColor: 'text-gray-700',
    badgeColor: 'bg-gray-100 text-gray-800',
    borderColor: 'border-gray-200',
    icon: '•'
  }
};

/**
 * Determines the status of a QueryTree based on its value and confidence.
 * Uses the same logic as backend RequirementBuilder.toJSON()
 */
export function getQueryTreeStatus(queryTree: QueryTreeJSON): QueryTreeStatus {
  // Handle non-boolean values
  if (queryTree.value !== 'true' && queryTree.value !== 'false') {
    return 'NON_BOOLEAN';
  }

  // Apply same logic as backend
  if (queryTree.confidence === 'RejectedResult') {
    return 'REJECTED';
  }

  if (queryTree.confidence === 'UndecidedResult') {
    return 'UNDECIDED';
  }

  if (queryTree.confidence === 'AcceptedResult') {
    return queryTree.value === 'true' ? 'FULFILLED' : 'VIOLATED';
  }

  return 'UNDECIDED';
}

/**
 * Gets the styling configuration for a QueryTree based on its status
 */
export function getQueryTreeStatusConfig(queryTree: QueryTreeJSON): QueryTreeStatusConfig {
  const status = getQueryTreeStatus(queryTree);
  return queryTreeStatusConfigs[status];
}
