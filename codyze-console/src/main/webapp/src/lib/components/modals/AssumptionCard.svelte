<script lang="ts">
  import type { AssumptionJSON } from '$lib/types';
  import QueryTreeNodeValue from '../analysis/QueryTreeNodeValue.svelte';

  interface Props {
    assumption: AssumptionJSON;
    onCopyId: (id: string) => void;
  }

  let { assumption, onCopyId }: Props = $props();

  // Get assumption status color
  function getAssumptionStatusColor(status: string): string {
    switch (status) {
      case 'Accepted':
        return 'text-green-700 bg-green-50 border-green-200';
      case 'Rejected':
        return 'text-red-700 bg-red-50 border-red-200';
      case 'Ignored':
        return 'text-gray-700 bg-gray-50 border-gray-200';
      case 'Undecided':
      default:
        return 'text-yellow-700 bg-yellow-50 border-yellow-200';
    }
  }

  // Get assumption type display name
  function getAssumptionTypeDisplay(type: string): string {
    return type.replace(/([A-Z])/g, ' $1').trim();
  }
</script>

<div class="border rounded-lg p-4 {getAssumptionStatusColor(assumption.status)}">
  <!-- Assumption Header -->
  <div class="flex items-start justify-between mb-2">
    <div class="flex items-center space-x-2">
      <span class="text-xs font-medium px-2 py-1 rounded border {getAssumptionStatusColor(assumption.status)}">
        {assumption.status}
      </span>
      <span class="text-xs text-gray-600">
        {getAssumptionTypeDisplay(assumption.assumptionType)}
      </span>
    </div>
    <button
      onclick={() => onCopyId(assumption.id)}
      class="flex items-center space-x-1 rounded px-2 py-1 text-xs text-gray-600 hover:bg-gray-100 hover:text-gray-800 transition-colors"
      title="Copy assumption ID"
    >
      <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
      </svg>
      <span>Copy ID</span>
    </button>
  </div>

  <!-- Assumption Message -->
  <div class="mb-3">
    <p class="text-sm leading-relaxed">
      {assumption.message}
    </p>
  </div>

  <!-- Assumption Details -->
  {#if assumption.node || assumption.nodeId || assumption.edgeLabel || assumption.assumptionScopeId}
    <div class="text-xs text-gray-600 space-y-2 border-t pt-3">
      {#if assumption.node}
        <div>
          <div class="font-medium mb-1 text-purple-700">🎯 Related Node:</div>
          <QueryTreeNodeValue node={assumption.node} />
        </div>
      {:else if assumption.nodeId}
        <div>
          <span class="font-medium">Related Node ID:</span>
          <span class="font-mono">{assumption.nodeId.substring(0, 8)}...</span>
        </div>
      {/if}
      {#if assumption.edgeLabel}
        <div>
          <span class="font-medium">Related Edge:</span>
          <span class="font-mono">{assumption.edgeLabel}</span>
        </div>
      {/if}
      {#if assumption.assumptionScopeId}
        <div>
          <span class="font-medium">Scope:</span>
          <span class="font-mono">{assumption.assumptionScopeId.substring(0, 8)}...</span>
        </div>
      {/if}
    </div>
  {/if}
</div>
