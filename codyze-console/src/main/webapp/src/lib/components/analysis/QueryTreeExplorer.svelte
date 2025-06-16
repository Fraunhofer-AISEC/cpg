<!-- QueryTreeExplorer.svelte -->
<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import { getQueryTreeStatusConfig, getQueryTreeStatus } from '$lib/types';
  import {
    loadQueryTrees,
    getCachedQueryTree,
    isQueryTreeLoading,
    getQueryTreeError
  } from '$lib/stores/queryTreeStore';
  import { getShortCallerInfo } from '$lib/utils/display';
  import QueryTreeExplorer from './QueryTreeExplorer.svelte';

  interface Props {
    queryTree: QueryTreeJSON | undefined;
    depth?: number;
  }

  let { queryTree, depth = 0 }: Props = $props();

  // Early return if queryTree is undefined
  if (!queryTree) {
    console.error('QueryTreeExplorer: queryTree prop is undefined');
  }

  // Use hasChildren from the QueryTree data structure
  const hasChildren = $derived(
    queryTree?.hasChildren === true && queryTree?.childrenIds && queryTree.childrenIds.length > 0
  );

  let isExpanded = $state(depth === 0); // Root node expanded by default
  let children = $state<QueryTreeJSON[]>([]);
  let loadingChildren = $state(false);
  let loadError = $state<string | null>(null);
  let childrenLoaded = $state(false); // Track if children have been loaded

  // Load children immediately if this is the root node and it has children
  $effect(() => {
    if (depth === 0 && hasChildren && !childrenLoaded && children.length === 0) {
      loadChildren();
    }
  });

  async function toggleExpanded() {
    if (!queryTree) return;

    isExpanded = !isExpanded;

    // Load children if expanding and not already loaded
    if (isExpanded && hasChildren && !childrenLoaded) {
      await loadChildren();
    }
  }

  async function loadChildren() {
    if (!queryTree?.childrenIds || queryTree.childrenIds.length === 0 || childrenLoaded) {
      return;
    }

    loadingChildren = true;
    loadError = null;

    try {
      const loadedChildren = await loadQueryTrees(queryTree.childrenIds);
      children = loadedChildren;
      childrenLoaded = true; // Mark as loaded to prevent re-loading
    } catch (error) {
      loadError = error instanceof Error ? error.message : 'Failed to load children';
      console.error('Failed to load QueryTree children:', error);
    } finally {
      loadingChildren = false;
    }
  }

  // Get status and styling configuration
  const statusConfig = $derived(queryTree ? getQueryTreeStatusConfig(queryTree) : null);
  const status = $derived(queryTree ? getQueryTreeStatus(queryTree) : null);
</script>

{#if !queryTree}
  <div class="rounded border border-red-200 bg-red-50 p-4 text-red-600">
    Error: QueryTree data is not available
  </div>
{:else}
  <div class="query-tree-node">
    <div
      class="mb-2 rounded-lg border p-3 {statusConfig?.bgColor} {statusConfig?.textColor} {statusConfig?.borderColor}"
    >
      <!-- Node header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          {#if hasChildren}
            <button
              onclick={toggleExpanded}
              class="hover:bg-opacity-50 rounded px-2 py-1 text-sm hover:bg-white"
            >
              {isExpanded ? '▼' : '▶'}
            </button>
          {:else}
            <div
              class="flex w-6 items-center justify-center text-xs {statusConfig?.textColor ||
                'text-gray-400'}"
            >
              ■
            </div>
          {/if}

          <div class="flex items-center space-x-2">
            <span class="rounded bg-blue-100 px-1.5 py-0.5 font-mono text-xs text-blue-700">
              {queryTree.queryTreeType}
            </span>
            <span class="bg-opacity-60 rounded bg-white px-2 py-1 font-mono text-xs">
              {queryTree.operator}
            </span>
            <div class="flex items-center space-x-1">
              <span class="text-lg">{statusConfig?.icon}</span>
              <span class="font-semibold">
                {queryTree.value}
              </span>
            </div>
          </div>
        </div>

        <div class="flex items-center space-x-2">
          <span class="bg-opacity-60 rounded bg-white px-2 py-1 text-xs">
            {queryTree.confidence}
          </span>
          {#if queryTree.nodeId}
            <span class="font-mono text-xs text-gray-500">
              Node: {queryTree.nodeId.slice(0, 8)}...
            </span>
          {/if}
          {#if queryTree.callerInfo}
            <span class="font-mono text-xs text-blue-600">
              📍 {getShortCallerInfo(
                queryTree.callerInfo.className,
                queryTree.callerInfo.methodName
              )}
            </span>
          {/if}
        </div>
      </div>

      <!-- String representation -->
      {#if queryTree.stringRepresentation}
        <div
          class="bg-opacity-40 mt-2 rounded border-l-2 border-current bg-white p-2 font-mono text-sm"
        >
          {queryTree.stringRepresentation}
        </div>
      {/if}

      <!-- Caller information -->
      {#if queryTree.callerInfo}
        <div class="bg-opacity-60 mt-2 rounded border-l-2 border-blue-300 bg-blue-50 p-2 text-xs">
          <div class="flex items-center space-x-1 text-blue-700">
            <span class="font-medium">📍 Called from:</span>
            <span class="font-mono">
              {queryTree.callerInfo.fileName}:{queryTree.callerInfo.lineNumber}
            </span>
          </div>
        </div>
      {/if}
    </div>

    <!-- Children (lazy loaded) -->
    {#if isExpanded && hasChildren}
      <div class="children">
        {#if loadingChildren}
          <div class="flex items-center space-x-2 p-2 text-sm text-gray-600">
            <div class="h-4 w-4 animate-spin rounded-full border-b-2 border-gray-600"></div>
            <span>Loading children...</span>
          </div>
        {:else if loadError}
          <div class="rounded border border-red-200 bg-red-50 p-2 text-sm text-red-600">
            <div class="font-medium">Failed to load children:</div>
            <div class="mt-1">{loadError}</div>
          </div>
        {:else}
          {#each children as child}
            <QueryTreeExplorer queryTree={child} depth={depth + 1} />
          {/each}
        {/if}
      </div>
    {/if}
  </div>
{/if}

<style>
  .query-tree-node {
    transition: all 0.2s ease;
  }

  .children {
    border-left: 2px solid #e5e7eb;
    position: relative;
    margin-left: 26px; /* Move line even further to the right */
    padding-left: 24px; /* Space between line and content */
  }

  .children::before {
    content: '';
    position: absolute;
    top: -8px;
    left: -2px;
    width: 2px;
    height: 16px;
    background: #e5e7eb;
  }
</style>
