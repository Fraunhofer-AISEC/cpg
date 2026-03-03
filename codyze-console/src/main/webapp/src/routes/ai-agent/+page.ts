import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import type { AnalysisResultJSON } from '$lib/types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    // Check if MCP module is enabled
    const featuresRes = await fetch('/api/features');
    if (featuresRes.ok) {
      const features = await featuresRes.json();
      if (!features.mcpEnabled) {
        // Redirect to dashboard if MCP is not enabled
        throw redirect(302, '/dashboard');
      }
    }

    const resultRes = await fetch('/api/result');
    const result: AnalysisResultJSON | null = resultRes.ok ? await resultRes.json() : null;

    return { result };
  } catch (error) {
    // Re-throw redirect errors
    if (error && typeof error === 'object' && 'status' in error) {
      throw error;
    }
    console.error('Error loading analysis result:', error);
    return { result: null };
  }
};
