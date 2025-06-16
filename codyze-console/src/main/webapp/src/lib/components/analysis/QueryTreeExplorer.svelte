<!-- QueryTreeExplorer.svelte -->
<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import { getQueryTreeStatusConfig, getQueryTreeStatus } from '$lib/types';
  import { loadQueryTrees, getCachedQueryTree, isQueryTreeLoading, getQueryTreeError } from '$lib/stores/queryTreeStore';
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
  const hasChildren = $derived(queryTree?.hasChildren === true && queryTree?.childrenIds && queryTree.childrenIds.length > 0);

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
  <div class="p-4 text-red-600 bg-red-50 border border-red-200 rounded">
    Error: QueryTree data is not available
  </div>
{:else}
  <div class="query-tree-node">
    <div class="border rounded-lg p-3 mb-2 {statusConfig?.bgColor} {statusConfig?.textColor} {statusConfig?.borderColor}">
      <!-- Node header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          {#if hasChildren}
            <button
              onclick={toggleExpanded}
              class="text-sm px-2 py-1 rounded hover:bg-white hover:bg-opacity-50"
            >
              {isExpanded ? '▼' : '▶'}
            </button>
          {:else}
            <div class="w-6 flex items-center justify-center text-xs {statusConfig?.textColor || 'text-gray-400'}">
              ■
            </div>
          {/if}
          
          <div class="flex items-center space-x-2">
            <span class="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-mono">
              {queryTree.queryTreeType}
            </span>
            <span class="text-xs font-mono px-2 py-1 rounded bg-white bg-opacity-60">
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
          <span class="text-xs px-2 py-1 rounded bg-white bg-opacity-60">
            {queryTree.confidence}
          </span>
          {#if queryTree.nodeId}
            <span class="text-xs text-gray-500 font-mono">
              Node: {queryTree.nodeId.slice(0, 8)}...
            </span>
          {/if}
          {#if queryTree.callerInfo}
            <span class="text-xs text-blue-600 font-mono">
              📍 {queryTree.callerInfo.className}.{queryTree.callerInfo.methodName}()
            </span>
          {/if}
        </div>
      </div>

      <!-- String representation -->
      {#if queryTree.stringRepresentation}
        <div class="mt-2 text-sm font-mono p-2 bg-white bg-opacity-40 rounded border-l-2 border-current">
          {queryTree.stringRepresentation}
        </div>
      {/if}

      <!-- Caller information -->
      {#if queryTree.callerInfo}
        <div class="mt-2 text-xs p-2 bg-blue-50 bg-opacity-60 rounded border-l-2 border-blue-300">
          <div class="flex items-center space-x-1 text-blue-700">
            <span class="font-medium">📍 Called from:</span>
            <span class="font-mono">{queryTree.callerInfo.className}.{queryTree.callerInfo.methodName}()</span>
          </div>
          <div class="text-blue-600 mt-1">
            <span class="font-mono">{queryTree.callerInfo.fileName}:{queryTree.callerInfo.lineNumber}</span>
          </div>
        </div>
      {/if}
    </div>

    <!-- Children (lazy loaded) -->
    {#if isExpanded && hasChildren}
      <div class="children">
        {#if loadingChildren}
          <div class="flex items-center space-x-2 p-2 text-sm text-gray-600">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-600"></div>
            <span>Loading children...</span>
          </div>
        {:else if loadError}
          <div class="p-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded">
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
