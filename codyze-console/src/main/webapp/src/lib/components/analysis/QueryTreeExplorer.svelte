<!-- QueryTreeExplorer.svelte -->
<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import { getQueryTreeStatusConfig, getQueryTreeStatus, getNodeLocation } from '$lib/types';
  import {
    loadQueryTrees,
    getCachedQueryTree,
    isQueryTreeLoading,
    getQueryTreeError
  } from '$lib/stores/queryTreeStore';
  import { getShortCallerInfo } from '$lib/utils/display';
  import QueryTreeExplorer from './QueryTreeExplorer.svelte';
  import QueryTreeNodeValue from './QueryTreeNodeValue.svelte';
  import AssumptionsModal from '../modals/AssumptionsModal.svelte';

  interface Props {
    queryTree: QueryTreeJSON | undefined;
    depth?: number;
    context?: 'requirements' | 'analysis';
    targetNodeId?: string; // ID of node to expand to
    pathToTarget?: Set<string>; // Set of node IDs that are on the path to the target
    baseUrl?: string; // Clean base URL for referrer without targetNodeId
  }

  let {
    queryTree,
    depth = 0,
    context = 'analysis',
    targetNodeId,
    pathToTarget,
    baseUrl
  }: Props = $props();

  // Early return if queryTree is undefined
  if (!queryTree) {
    console.error('QueryTreeExplorer: queryTree prop is undefined');
  }

  // Use hasChildren from the QueryTree data structure
  const hasChildren = $derived(
    queryTree?.hasChildren === true && queryTree?.childrenIds && queryTree.childrenIds.length > 0
  );

  // Determine if this node should be expanded based on whether it's on the path to target
  const shouldExpand = $derived(() => {
    if (!queryTree) return false;

    // Always expand root node
    if (depth === 0) return true;

    // Expand if this node is on the path to the target
    return pathToTarget?.has(queryTree.id) || false;
  });

  let isExpanded = $state(false);
  let children = $state<QueryTreeJSON[]>([]);
  let loadingChildren = $state(false);
  let loadError = $state<string | null>(null);
  let childrenLoaded = $state(false); // Track if children have been loaded
  let showCallerDetails = $state(false); // Track if caller details should be shown
  let showAssumptionsModal = $state(false); // Track if assumptions modal should be shown

  // Progressive loading state
  const BATCH_SIZE = 10;
  let loadedCount = $state(0);
  let loadingMore = $state(false);

  // Toast notification state
  let showToast = $state(false);
  let toastMessage = $state('');

  // Element reference for scrolling
  let nodeElement: HTMLDivElement | undefined = $state();

  // Update expanded state when shouldExpand changes
  $effect(() => {
    isExpanded = shouldExpand();
  });

  // Load children immediately if this node should be expanded and has children
  $effect(() => {
    if (isExpanded && hasChildren && !childrenLoaded && children.length === 0) {
      loadChildren();
    }
  });

  // Auto-scroll to target node when it becomes available
  $effect(() => {
    if (queryTree?.id === targetNodeId && nodeElement) {
      // Use a small delay to ensure DOM is fully updated after expansion
      setTimeout(() => {
        if (nodeElement) {
          nodeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    }
  });

  // Also scroll when pathToTarget changes and we find our target
  $effect(() => {
    if (queryTree?.id === targetNodeId && nodeElement && pathToTarget?.has(queryTree?.id || '')) {
      // Wait a bit longer for children to load and DOM to update
      setTimeout(() => {
        if (nodeElement) {
          nodeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 200);
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
      // Load initial batch
      const initialBatch = queryTree.childrenIds.slice(0, BATCH_SIZE);
      const loadedChildren = await loadQueryTrees(initialBatch);
      children = loadedChildren;
      loadedCount = initialBatch.length;
      childrenLoaded = true; // Mark as loaded to prevent re-loading
    } catch (error) {
      loadError = error instanceof Error ? error.message : 'Failed to load children';
      console.error('Failed to load QueryTree children:', error);
    } finally {
      loadingChildren = false;
    }
  }

  // Function to load more children
  async function loadMoreChildren() {
    if (!queryTree?.childrenIds || loadingMore || loadedCount >= queryTree.childrenIds.length) {
      return;
    }

    try {
      loadingMore = true;
      const nextBatch = queryTree.childrenIds.slice(loadedCount, loadedCount + BATCH_SIZE);
      const newChildren = await loadQueryTrees(nextBatch);
      children = [...children, ...newChildren];
      loadedCount += nextBatch.length;
    } catch (error) {
      loadError = error instanceof Error ? error.message : 'Failed to load more children';
      console.error('Failed to load more QueryTree children:', error);
    } finally {
      loadingMore = false;
    }
  }

  // Show toast notification
  function showToastNotification(message: string) {
    toastMessage = message;
    showToast = true;

    // Auto-hide after 2 seconds
    setTimeout(() => {
      showToast = false;
    }, 2000);
  }

  async function copyDeepLink() {
    if (!queryTree?.id || typeof window === 'undefined') return;

    try {
      // Create URL with targetNodeId for this QueryTree node
      const url = new URL(window.location.href);
      url.searchParams.set('targetNodeId', queryTree.id);

      // Copy to clipboard
      await navigator.clipboard.writeText(url.toString());

      // Show success toast
      showToastNotification('Link copied to clipboard!');
    } catch (error) {
      console.error('Failed to copy deep link:', error);
      // Show error toast
      showToastNotification('Failed to copy link');

      // Fallback: select the URL in address bar (if possible)
      try {
        const url = new URL(window.location.href);
        url.searchParams.set('targetNodeId', queryTree.id);
        window.history.replaceState(null, '', url.toString());
      } catch (fallbackError) {
        console.error('Fallback also failed:', fallbackError);
      }
    }
  }

  // Open assumptions modal
  function openAssumptionsModal() {
    showAssumptionsModal = true;
  }

  // Close assumptions modal
  function closeAssumptionsModal() {
    showAssumptionsModal = false;
  }

  // Navigate to a child QueryTree node
  function navigateToChild(childId: string) {
    if (!baseUrl) return;

    const url = new URL(baseUrl, window.location.origin);
    url.searchParams.set('targetNodeId', childId);
    window.location.href = url.toString();
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
      bind:this={nodeElement}
      id="query-tree-node-{queryTree.id}"
      data-query-tree-id={queryTree.id}
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
                {#if queryTree.nodeValues}
                  {queryTree.nodeValues.length} node(s)
                {:else}
                  {queryTree.value}
                {/if}
              </span>
            </div>
          </div>
        </div>

        <div class="flex items-center space-x-2">
          <button
            onclick={() => openAssumptionsModal()}
            class="bg-opacity-60 cursor-pointer rounded bg-white px-2 py-1 text-xs transition-colors hover:bg-gray-100"
            title="Click to view assumptions for this query tree"
          >
            {queryTree.confidence}
            {#if queryTree.assumptions && queryTree.assumptions.length > 0}
              <span
                class="ml-1 text-orange-600"
                title="{queryTree.assumptions.length} assumption(s)"
              >
                ⚠
              </span>
            {/if}
          </button>
          {#if context === 'requirements'}
            <button
              onclick={() => copyDeepLink()}
              class="rounded bg-blue-50 px-2 py-1 text-xs text-blue-600 transition-colors hover:bg-blue-100"
              title="Copy link to this QueryTree node"
            >
              🔗 Copy Link
            </button>
          {/if}
          {#if queryTree.callerInfo}
            <button
              onclick={() => {
                showCallerDetails = !showCallerDetails;
              }}
              class="cursor-pointer font-mono text-xs text-blue-600 hover:text-blue-800 hover:underline"
              title="Click to {showCallerDetails ? 'hide' : 'show'} file location"
            >
              {#if showCallerDetails}
                {getShortCallerInfo(
                  queryTree.callerInfo.className,
                  queryTree.callerInfo.methodName
                )} in {queryTree.callerInfo.fileName}:{queryTree.callerInfo.lineNumber}
              {:else}
                {getShortCallerInfo(
                  queryTree.callerInfo.className,
                  queryTree.callerInfo.methodName
                )}
              {/if}
            </button>
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

      <!-- Node (if present) -->
      {#if queryTree.node}
        <div
          class="bg-opacity-60 mt-2 rounded border-l-2 border-purple-300 bg-purple-50 p-2 text-xs"
        >
          <div class="mb-2 font-medium text-purple-700">🎯 Associated Node:</div>
          <QueryTreeNodeValue node={queryTree.node} {baseUrl} queryTreeId={queryTree.id} />
        </div>
      {/if}

      <!-- Node Values (if present) -->
      {#if queryTree.nodeValues && queryTree.nodeValues.length > 0}
        <div
          class="bg-opacity-60 mt-2 rounded border-l-2 border-purple-300 bg-purple-50 p-2 text-xs"
        >
          <div class="mb-2 font-medium text-purple-700">
            🔗 Found Nodes ({queryTree.nodeValues.length}):
          </div>
          <div class="space-y-1">
            {#each queryTree.nodeValues as node}
              <QueryTreeNodeValue {node} {baseUrl} queryTreeId={queryTree?.id} />
            {/each}
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
            <QueryTreeExplorer
              queryTree={child}
              depth={depth + 1}
              {context}
              {targetNodeId}
              {pathToTarget}
              {baseUrl}
            />
          {/each}
          
          <!-- Load more button or completion message -->
          {#if queryTree?.childrenIds && loadedCount < queryTree.childrenIds.length}
            <div class="ml-6 mt-4 text-center">
              <button
                onclick={loadMoreChildren}
                disabled={loadingMore}
                class="inline-flex items-center rounded-md border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {#if loadingMore}
                  <div class="mr-2 h-4 w-4 animate-spin rounded-full border-b-2 border-gray-600"></div>
                  Loading...
                {:else}
                  Load more children ({loadedCount} of {queryTree.childrenIds.length} loaded)
                {/if}
              </button>
            </div>
          {:else if queryTree?.childrenIds && queryTree.childrenIds.length > BATCH_SIZE}
            <div class="ml-6 mt-4 text-center text-sm text-gray-600">
              All {queryTree.childrenIds.length} children loaded
            </div>
          {/if}
        {/if}
      </div>
    {/if}
  </div>
{/if}

<!-- Toast Notification -->
{#if showToast}
  <div class="toast-notification">
    {toastMessage}
  </div>
{/if}

<!-- Assumptions Modal -->
<AssumptionsModal
  {queryTree}
  assumptions={queryTree?.assumptions || []}
  isOpen={showAssumptionsModal}
  onClose={closeAssumptionsModal}
  {baseUrl}
  requirementId={context === 'requirements' ? baseUrl?.split('/').pop() : undefined}
/>

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

  .toast-notification {
    position: fixed;
    top: 20px;
    right: 20px;
    background: #10b981;
    color: white;
    padding: 12px 20px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 1000;
    font-size: 14px;
    font-weight: 500;
    animation: slideIn 0.3s ease-out;
  }

  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
</style>
