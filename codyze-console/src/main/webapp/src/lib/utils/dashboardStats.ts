import type { AnalysisResultJSON, AnalysisProjectJSON, RequirementsCategoryJSON } from '$lib/types';

export interface StatItem {
  title: string;
  value: string | number;
}

/**
 * Calculate requirement fulfillment statistics
 */
export function calculateFulfillmentStats(requirementCategories?: RequirementsCategoryJSON[]) {
  if (!requirementCategories) {
    return {
      total: 0,
      fulfilled: 0,
      notFulfilled: 0,
      rejected: 0,
      undecided: 0,
      notYetEvaluated: 0
    };
  }

  return {
    total: requirementCategories.reduce(
      (acc, cat) => acc + cat.requirements.length,
      0
    ),
    fulfilled: requirementCategories.reduce(
      (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'FULFILLED').length,
      0
    ),
    notFulfilled: requirementCategories.reduce(
      (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'NOT_FULFILLED').length,
      0
    ),
    rejected: requirementCategories.reduce(
      (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'REJECTED').length,
      0
    ),
    undecided: requirementCategories.reduce(
      (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'UNDECIDED').length,
      0
    ),
    notYetEvaluated: requirementCategories.reduce(
      (acc, cat) =>
        acc + cat.requirements.filter((r) => r.status === 'NOT_YET_EVALUATED').length,
      0
    )
  };
}

/**
 * Calculate combined project and source code statistics
 */
export function calculateCombinedProjectStats(
  project?: AnalysisProjectJSON,
  result?: AnalysisResultJSON
): StatItem[] {
  const stats: StatItem[] = [];
  
  // Project info
  if (project) {
    stats.push(
      { title: 'Project Name', value: project.name },
      { title: 'Created', value: new Date(project.projectCreatedAt).toLocaleString() }
    );
    
    if (project.lastAnalyzedAt) {
      stats.push({
        title: 'Last Analyzed',
        value: new Date(project.lastAnalyzedAt).toLocaleString()
      });
    }
  }
  
  // Source code info with additional calculated metrics
  if (result?.components) {
    const totalTUs = result.components.reduce(
      (acc, comp) => acc + comp.translationUnits.length,
      0
    );
    
    const avgTUsPerComponent = totalTUs > 0 ? 
      Math.round((totalTUs / result.components.length) * 10) / 10 : 0;
    
    const avgNodesPerTU = totalTUs > 0 ? 
      Math.round((result.totalNodes / totalTUs) * 10) / 10 : 0;
    
    // Find largest component
    const largestComponent = result.components.reduce((max, comp) => 
      comp.translationUnits.length > max.translationUnits.length ? comp : max
    );
    
    stats.push(
      { title: 'Components', value: result.components.length },
      { title: 'Translation Units', value: totalTUs },
      { title: 'Total Nodes', value: result.totalNodes.toLocaleString() },
      { title: 'Avg TUs per Component', value: avgTUsPerComponent },
      { title: 'Avg Nodes per TU', value: avgNodesPerTU },
      { 
        title: 'Largest Component', 
        value: `${largestComponent.name} (${largestComponent.translationUnits.length} files)` 
      }
    );
  }
  
  return stats;
}
