import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import type { AnalysisResultJSON, McpCapabilities } from '$lib/types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    const featuresRes = await fetch('/api/features');
    if (featuresRes.ok) {
      const features = await featuresRes.json();
      if (!features.mcpEnabled) {
        throw redirect(302, '/dashboard');
      }
    }

    const [resultRes, capsRes] = await Promise.all([
      fetch('/api/result'),
      fetch('/api/chat/mcp/capabilities')
    ]);

    const result: AnalysisResultJSON | null = resultRes.ok ? await resultRes.json().catch(() => null) : null;
    const mcpCapabilities: McpCapabilities | null = capsRes.ok ? await capsRes.json().catch(() => null) : null;

    return { result, mcpCapabilities };
  } catch (error) {
    if (error && typeof error === 'object' && 'status' in error) throw error;
    console.error('Error loading chat page:', error);
    return { result: null, mcpCapabilities: null };
  }
};
