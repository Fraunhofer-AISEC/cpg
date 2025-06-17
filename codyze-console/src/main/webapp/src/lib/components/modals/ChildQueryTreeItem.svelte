<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import { getQueryTreeStatusConfig } from '$lib/types';
  import { getShortCallerInfo } from '$lib/utils/display';
  import QueryTreeNodeValue from '$lib/components/analysis/QueryTreeNodeValue.svelte';
  import ConfidencePill from '$lib/components/ui/ConfidencePill.svelte';

  interface Props {
    queryTree: QueryTreeJSON;
    baseUrl?: string;
    href: string;
  }

  let { queryTree, baseUrl, href }: Props = $props();

  const statusConfig = $derived(getQueryTreeStatusConfig(queryTree));
</script>

<a
  {href}
  class="block rounded-lg border p-4 transition-all group hover:shadow-md {statusConfig.bgColor} {statusConfig.textColor} {statusConfig.borderColor}"
  title="Click to view child evaluation with assumptions"
>
  <!-- Header with status, type, and operator -->
  <div class="flex items-center justify-between mb-3">
    <div class="flex items-center space-x-2 min-w-0 flex-1">
      <!-- Status pill -->
      <ConfidencePill confidence={queryTree.confidence} size="sm" />
      
      <span class="rounded bg-blue-100 px-1.5 py-0.5 font-mono text-xs text-blue-800 bg-opacity-80">
        {queryTree.queryTreeType}
      </span>
      
      <span class="rounded bg-white bg-opacity-80 px-2 py-1 font-mono text-xs border border-gray-300">
        {queryTree.operator}
      </span>
    </div>
    
    <svg class="h-4 w-4 text-gray-400 group-hover:text-gray-600 transition-colors flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5-5 5M6 12h12" />
    </svg>
  </div>

  <!-- Result value and assumptions -->
  <div class="flex items-center justify-between mb-3">
    <div class="flex items-center space-x-2">
      <span class="text-lg">{statusConfig.icon}</span>
      <span class="font-semibold text-sm">
        {#if queryTree.nodeValues}
          {queryTree.nodeValues.length} node(s)
        {:else}
          {queryTree.value}
        {/if}
      </span>
    </div>
    
    {#if queryTree.assumptions && queryTree.assumptions.length > 0}
      <div class="flex items-center space-x-1 bg-orange-50 rounded px-2 py-1 text-xs border border-orange-200">
        <span class="text-orange-600">⚠</span>
        <span class="text-orange-700 font-medium">{queryTree.assumptions.length} assumption{queryTree.assumptions.length !== 1 ? 's' : ''}</span>
      </div>
    {/if}
  </div>

  <!-- Caller info (if available) -->
  {#if queryTree.callerInfo}
    <div class="text-xs opacity-80 mb-2">
      <span class="font-mono">
        {getShortCallerInfo(queryTree.callerInfo.className, queryTree.callerInfo.methodName)}
      </span>
      <span class="opacity-75">
        in {queryTree.callerInfo.fileName}:{queryTree.callerInfo.lineNumber}
      </span>
    </div>
  {/if}

  <!-- String representation (if available) -->
  {#if queryTree.stringRepresentation}
    <div class="mt-2 rounded border-l-2 border-current bg-black bg-opacity-5 p-2 font-mono text-xs">
      {queryTree.stringRepresentation}
    </div>
  {/if}

  <!-- Associated Node (if present) -->
  {#if queryTree.node}
    <div class="mt-2 rounded border-l-2 border-purple-400 bg-purple-50 bg-opacity-80 p-2 text-xs">
      <div class="mb-2 font-medium text-purple-700">🎯 Associated Node:</div>
      <QueryTreeNodeValue node={queryTree.node} {baseUrl} queryTreeId={queryTree.id} />
    </div>
  {/if}
</a>
