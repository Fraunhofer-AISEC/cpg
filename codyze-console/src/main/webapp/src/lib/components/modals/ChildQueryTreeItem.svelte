<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import { getQueryTreeStatusConfig } from '$lib/types';
  import { getShortCallerInfo } from '$lib/utils/display';
  import QueryTreeNodeValue from '$lib/components/analysis/QueryTreeNodeValue.svelte';
  import ConfidencePill from '$lib/components/ui/ConfidencePill.svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';

  interface Props {
    queryTree: QueryTreeJSON;
    baseUrl?: string;
    href: string;
    onNavigate?: () => void; // Callback to close modal before navigation
  }

  let { queryTree, baseUrl, href, onNavigate }: Props = $props();

  const statusConfig = $derived(getQueryTreeStatusConfig(queryTree));

  // Handle navigation with modal close
  function handleNavigation(event: MouseEvent) {
    event.preventDefault();

    // Close modal if callback provided
    if (onNavigate) {
      onNavigate();
    }

    // Check if we're navigating to the same targetNodeId
    const url = new URL(href);
    const targetNodeId = url.searchParams.get('targetNodeId');
    const currentTargetNodeId = $page.url.searchParams.get('targetNodeId');

    if (targetNodeId === currentTargetNodeId && targetNodeId) {
      // Same target - just scroll to it without navigation
      scrollToTargetNode(targetNodeId);
    } else {
      // Different target - navigate normally
      goto(href);
    }
  }

  // Function to scroll to a specific node
  function scrollToTargetNode(nodeId: string) {
    // Use a small delay to ensure modal is closed first
    setTimeout(() => {
      const element = document.querySelector(`[data-query-tree-id="${nodeId}"]`);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
  }
</script>

<a
  {href}
  onclick={handleNavigation}
  class="group block rounded-lg border p-4 transition-all hover:shadow-md {statusConfig.bgColor} {statusConfig.textColor} {statusConfig.borderColor}"
  title="Click to view child evaluation with assumptions"
>
  <!-- Header with status, type, and operator -->
  <div class="mb-3 flex items-center justify-between">
    <div class="flex min-w-0 flex-1 items-center space-x-2">
      <!-- Status pill -->
      <ConfidencePill confidence={queryTree.confidence} size="sm" />

      <span class="bg-opacity-80 rounded bg-blue-100 px-1.5 py-0.5 font-mono text-xs text-blue-800">
        {queryTree.queryTreeType}
      </span>

      <span
        class="bg-opacity-80 rounded border border-gray-300 bg-white px-2 py-1 font-mono text-xs"
      >
        {queryTree.operator}
      </span>
    </div>

    <svg
      class="h-4 w-4 flex-shrink-0 text-gray-400 transition-colors group-hover:text-gray-600"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M13 7l5 5-5 5M6 12h12"
      />
    </svg>
  </div>

  <!-- Result value and assumptions -->
  <div class="mb-3 flex items-center justify-between">
    <div class="flex items-center space-x-2">
      <span class="text-lg">{statusConfig.icon}</span>
      <span class="text-sm font-semibold">
        {#if queryTree.nodeValues}
          {queryTree.nodeValues.length} node(s)
        {:else}
          {queryTree.value}
        {/if}
      </span>
    </div>

    {#if queryTree.assumptions && queryTree.assumptions.length > 0}
      <div
        class="flex items-center space-x-1 rounded border border-orange-200 bg-orange-50 px-2 py-1 text-xs"
      >
        <span class="text-orange-600">⚠</span>
        <span class="font-medium text-orange-700"
          >{queryTree.assumptions.length} assumption{queryTree.assumptions.length !== 1
            ? 's'
            : ''}</span
        >
      </div>
    {/if}
  </div>

  <!-- Caller info (if available) -->
  {#if queryTree.callerInfo}
    <div class="mb-2 text-xs opacity-80">
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
    <div class="bg-opacity-5 mt-2 rounded border-l-2 border-current bg-black p-2 font-mono text-xs">
      {queryTree.stringRepresentation}
    </div>
  {/if}

  <!-- Associated Node (if present) -->
  {#if queryTree.node}
    <div class="bg-opacity-80 mt-2 rounded border-l-2 border-purple-400 bg-purple-50 p-2 text-xs">
      <div class="mb-2 font-medium text-purple-700">🎯 Associated Node:</div>
      <QueryTreeNodeValue node={queryTree.node} {baseUrl} queryTreeId={queryTree.id} />
    </div>
  {/if}
</a>
