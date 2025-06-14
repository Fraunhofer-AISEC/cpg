import type { PageLoad } from './$types';
import type { AnalysisResultJSON } from '$lib/types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    const response = await fetch('/api/result');

    if (response.ok) {
      const result: AnalysisResultJSON = await response.json();
      return { result };
    }

    return { result: null };
  } catch (error) {
    console.error('Error loading source code data:', error);
    return { result: null };
  }
};
