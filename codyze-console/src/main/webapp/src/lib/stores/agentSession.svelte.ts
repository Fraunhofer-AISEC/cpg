import type { McpCapabilities, SkillInfo, LlmProviderWithModels } from '$lib/types';

class AgentSession {
  mcpCapabilities = $state<McpCapabilities | null>(null);
  skills = $state<SkillInfo[]>([]);
  showMcpModal = $state(false);
  showSkillsModal = $state(false);
  providers = $state<LlmProviderWithModels[]>([]);
  private providersLoaded = false;

  init(mcpCapabilities: McpCapabilities | null, skills: SkillInfo[]) {
    this.mcpCapabilities = mcpCapabilities;
    this.skills = skills;
  }

  hasCachedProviders(): boolean {
    return this.providersLoaded;
  }

  setProviders(providers: LlmProviderWithModels[]) {
    this.providers = providers;
    this.providersLoaded = true;
  }

  openMcpModal = () => { this.showMcpModal = true; };
  closeMcpModal = () => { this.showMcpModal = false; };
  openSkillsModal = () => { this.showSkillsModal = true; };
  closeSkillsModal = () => { this.showSkillsModal = false; };
}

export const agentSession = new AgentSession();