import type { McpCapabilities, SkillInfo } from '$lib/types';

class AgentSession {
  mcpCapabilities = $state<McpCapabilities | null>(null);
  skills = $state<SkillInfo[]>([]);
  showMcpModal = $state(false);
  showSkillsModal = $state(false);

  init(mcpCapabilities: McpCapabilities | null, skills: SkillInfo[]) {
    this.mcpCapabilities = mcpCapabilities;
    this.skills = skills;
  }

  openMcpModal = () => { this.showMcpModal = true; };
  closeMcpModal = () => { this.showMcpModal = false; };
  openSkillsModal = () => { this.showSkillsModal = true; };
  closeSkillsModal = () => { this.showSkillsModal = false; };
}

export const agentSession = new AgentSession();