import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import type { AnalysisResultJSON, LlmProviderWithModels, McpCapabilities, SkillInfo } from '$lib/types';
import { agentSession } from '$lib/stores/agentSession.svelte';

export const load: PageLoad = async ({ fetch }) => {
  try {
    const featuresRes = await fetch('/api/features');
    if (featuresRes.ok) {
      const features = await featuresRes.json();
      if (!features.mcpEnabled) {
        throw redirect(302, '/dashboard');
      }
    }

    async function loadProviders(): Promise<LlmProviderWithModels[]> {
      if (agentSession.hasCachedProviders()) return agentSession.providers;
      try {
        const res = await fetch('/api/chat/providers');
        const providers: LlmProviderWithModels[] = res.ok ? await res.json().catch(() => []) : [];
        agentSession.setProviders(providers);
        return providers;
      } catch {
        return [];
      }
    }

    const [resultRes, capsRes, skillsRes, providers] = await Promise.all([
      fetch('/api/result'),
      fetch('/api/chat/mcp/capabilities'),
      fetch('/api/chat/skills'),
      loadProviders()
    ]);

    const result: AnalysisResultJSON | null = resultRes.ok ? await resultRes.json().catch(() => null) : null;
    const mcpCapabilities: McpCapabilities | null = capsRes.ok ? await capsRes.json().catch(() => null) : null;
    const skills: SkillInfo[] = skillsRes.ok ? await skillsRes.json().catch(() => []) : [];

    return { result, mcpCapabilities, skills, providers };
  } catch (error) {
    if (error && typeof error === 'object' && 'status' in error) throw error;
    console.error('Error loading chat page:', error);
    return { result: null, mcpCapabilities: null, skills: [], providers: [] };
  }
};