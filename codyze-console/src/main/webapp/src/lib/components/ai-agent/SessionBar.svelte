<script lang="ts">
  import { agentSession } from '$lib/stores/agentSession.svelte';
</script>

{#if agentSession.mcpCapabilities || agentSession.skills.length > 0}
  <div class="flex items-center gap-1.5">
    {#if agentSession.mcpCapabilities}
      <button
        class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
        onclick={agentSession.openMcpModal}
        title="MCP Server"
      >
        <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
        {agentSession.mcpCapabilities.serverName}
      </button>
    {/if}
    {#if agentSession.skills.length > 0}
      <button
        class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
        onclick={agentSession.openSkillsModal}
        title="Skills"
      >
        <svg class="h-3 w-3 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        {agentSession.skills.length} skill{agentSession.skills.length === 1 ? '' : 's'}
      </button>
    {/if}
  </div>
{/if}