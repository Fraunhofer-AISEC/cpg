import type { AnalysisResultJSON } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
  const res = await fetch(`/api/result`);
  const result: AnalysisResultJSON = await res.json();

  return { result };
};
