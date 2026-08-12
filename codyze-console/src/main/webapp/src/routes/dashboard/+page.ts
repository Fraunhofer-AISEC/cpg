import type { PageLoad } from './$types';
import type { AnalysisProjectJSON, AnalysisResultJSON } from '$lib/types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    // Load both project info and analysis result in parallel
    const [projectRes, resultRes] = await Promise.all([
      fetch('/api/project'),
      fetch('/api/result')
    ]);

    const project: AnalysisProjectJSON | null = projectRes.ok ? await projectRes.json() : null;
    const result: AnalysisResultJSON | null = resultRes.ok ? await resultRes.json() : null;

    return { project, result };
  } catch (error) {
    console.error('Error loading dashboard data:', error);
    return { project: null, result: null };
  }
};
