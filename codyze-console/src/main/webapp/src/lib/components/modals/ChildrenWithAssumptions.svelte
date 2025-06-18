<script lang="ts">
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { loadQueryTrees } from '$lib/stores/queryTreeStore';
  import type { QueryTreeJSON, AssumptionJSON } from '$lib/types';
  import ChildQueryTreeItem from './ChildQueryTreeItem.svelte';
  import LoadMoreButton from '../ui/LoadMoreButton.svelte';

  interface Props {
    childrenWithAssumptionIds: Record<string, string[]>;
    baseUrl?: string;
    requirementId?: string;
    onNavigate?: () => void; // Callback to close modal before navigation
  }

  let { childrenWithAssumptionIds, baseUrl, requirementId, onNavigate }: Props = $props();

  // Grouped data structure
  interface AssumptionGroup {
    assumptionId: string;
    assumption?: AssumptionJSON;
    childrenIds: string[];
    loadedChildren: QueryTreeJSON[];
    loadedCount: number;
    loading: boolean;
    loadingMore: boolean;
    error: string | null;
  }

  // State for assumption groups
  let assumptionGroups = $state<AssumptionGroup[]>([]);
  let loading = $state(true);
  let globalError = $state<string | null>(null);

  // Pagination constants
  const BATCH_SIZE = 10;

  // Initialize assumption groups from the map
  const totalChildrenCount = $derived(() => {
    return Object.values(childrenWithAssumptionIds).reduce((sum, children) => sum + children.length, 0);
  });

  // Load query tree data on mount
  onMount(async () => {
    if (Object.keys(childrenWithAssumptionIds).length === 0) {
      loading = false;
      return;
    }

    try {
      loading = true;
      globalError = null;

      // Initialize assumption groups with deduplication
      assumptionGroups = Object.entries(childrenWithAssumptionIds).map(([assumptionId, childrenIds]) => ({
        assumptionId,
        assumption: undefined, // Will be populated when we load the first child
        childrenIds: [...new Set(childrenIds)], // Deduplicate children IDs
        loadedChildren: [],
        loadedCount: 0,
        loading: false,
        loadingMore: false,
        error: null
      }));

      // Load the first batch for each assumption group
      await Promise.all(assumptionGroups.map(group => loadMoreForGroup(group)));
    } catch (err) {
      console.error('Failed to initialize children with assumptions:', err);
      globalError = 'Failed to load child evaluation details';
    } finally {
      loading = false;
    }
  });

  // Function to load more children for a specific assumption group
  async function loadMoreForGroup(group: AssumptionGroup) {
    if (group.loadingMore || group.loadedCount >= group.childrenIds.length) return;

    try {
      group.loadingMore = true;
      group.error = null;

      const nextBatch = group.childrenIds.slice(group.loadedCount, group.loadedCount + BATCH_SIZE);
      const newQueryTrees = await loadQueryTrees(nextBatch);
      
      group.loadedChildren = [...group.loadedChildren, ...newQueryTrees];
      group.loadedCount += nextBatch.length;

      // If we don't have the assumption details yet, get it from the first loaded child
      if (!group.assumption && newQueryTrees.length > 0) {
        const firstChild = newQueryTrees[0];
        group.assumption = firstChild.assumptions.find(a => a.id === group.assumptionId);
      }
    } catch (err) {
      console.error(`Failed to load more children for assumption ${group.assumptionId}:`, err);
      group.error = 'Failed to load additional child evaluations';
    } finally {
      group.loadingMore = false;
    }
  }

  function getChildLink(childId: string): string {
    // Use baseUrl if provided, otherwise use current page
    const urlString = baseUrl || $page.url.pathname;
    const url = new URL(urlString, $page.url.origin);
    url.searchParams.set('targetNodeId', childId);
    return url.toString();
  }

  function getShortId(id: string): string {
    return id.substring(0, 8);
  }

  // Function to handle navigation with same-target detection
  function handleFallbackNavigation(event: MouseEvent, childId: string) {
    event.preventDefault();

    // Close modal if callback provided
    if (onNavigate) {
      onNavigate();
    }

    // Check if we're navigating to the same targetNodeId
    const url = new URL(getChildLink(childId));
    const targetNodeId = url.searchParams.get('targetNodeId');
    const currentTargetNodeId = $page.url.searchParams.get('targetNodeId');

    if (targetNodeId === currentTargetNodeId && targetNodeId) {
      // Same target - just scroll to it without navigation
      setTimeout(() => {
        const element = document.querySelector(`[data-query-tree-id="${targetNodeId}"]`);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    } else {
      // Different target - navigate normally
      goto(getChildLink(childId));
    }
  }

  // Get assumption type display info
  function getAssumptionTypeInfo(assumptionType: string) {
    switch (assumptionType) {
      case 'EDGE_LABEL':
        return { icon: '🔗', label: 'Edge Label' };
      case 'NODE_TYPE':
        return { icon: '📝', label: 'Node Type' };
      case 'VALUE':
        return { icon: '💰', label: 'Value' };
      default:
        return { icon: '❓', label: assumptionType };
    }
  }
</script>

{#if totalChildrenCount() > 0}
  <div class="mt-4 border-t pt-4">
    <h4 class="mb-3 text-sm font-semibold text-gray-900">🔗 Propagated from Children</h4>
    <div class="mb-3 text-xs text-gray-600">
      <p>This result is influenced by assumptions made in the following child evaluations:</p>
    </div>

    {#if loading}
      <div class="flex items-center justify-center py-4">
        <div class="h-5 w-5 animate-spin rounded-full border-b-2 border-blue-600"></div>
        <span class="ml-2 text-sm text-gray-600">Loading child evaluations...</span>
      </div>
    {:else if globalError}
      <div class="py-4 text-center text-sm text-red-600">
        <p>⚠️ {globalError}</p>
        <p class="mt-2 text-gray-600">Showing grouped children IDs only:</p>
        <div class="mt-2 space-y-3">
          {#each Object.entries(childrenWithAssumptionIds) as [assumptionId, childrenIds]}
            <div class="rounded border border-gray-200 bg-gray-50 p-3">
              <div class="mb-2 text-xs font-medium text-gray-700">
                Assumption: {getShortId(assumptionId)}...
              </div>
              <div class="space-y-1">
                {#each childrenIds as childId}
                  <a
                    href={getChildLink(childId)}
                    onclick={(e) => handleFallbackNavigation(e, childId)}
                    class="block flex w-full items-center space-x-2 rounded border border-gray-200 bg-white px-2 py-1 text-left text-xs transition-colors hover:border-gray-300 hover:bg-gray-100"
                  >
                    <span class="font-mono text-gray-800">{getShortId(childId)}...</span>
                    <span class="text-gray-600">View evaluation</span>
                  </a>
                {/each}
              </div>
            </div>
          {/each}
        </div>
      </div>
    {:else}
      <div class="space-y-4">
        {#each assumptionGroups as group (group.assumptionId)}
          <div class="rounded-lg border border-gray-200 bg-gray-50 p-4">
            <!-- Assumption Header -->
            <div class="mb-3 border-b border-gray-200 pb-2">
              <div class="flex items-center space-x-2">
                {#if group.assumption}
                  {@const typeInfo = getAssumptionTypeInfo(group.assumption.assumptionType)}
                  <span class="text-lg">{typeInfo.icon}</span>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-gray-900">
                      {typeInfo.label} Assumption
                    </div>
                    <div class="text-xs text-gray-600">
                      {group.assumption.message}
                    </div>
                  </div>
                {:else}
                  <span class="text-lg">❓</span>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-gray-900">
                      Assumption
                    </div>
                    <div class="text-xs text-gray-600 font-mono">
                      ID: {getShortId(group.assumptionId)}...
                    </div>
                  </div>
                {/if}
                <div class="text-xs text-gray-500">
                  {group.childrenIds.length} child{group.childrenIds.length !== 1 ? 'ren' : ''}
                </div>
              </div>
            </div>

            <!-- Children List -->
            {#if group.error && group.loadedChildren.length === 0}
              <div class="py-2 text-center text-sm text-red-600">
                <p>⚠️ {group.error}</p>
                <p class="mt-2 text-gray-600">Showing child IDs only:</p>
                <div class="mt-2 space-y-1">
                  {#each group.childrenIds as childId}
                    <a
                      href={getChildLink(childId)}
                      onclick={(e) => handleFallbackNavigation(e, childId)}
                      class="block flex w-full items-center space-x-2 rounded border border-gray-200 bg-white px-2 py-1 text-left text-xs transition-colors hover:border-gray-300 hover:bg-gray-100"
                    >
                      <span class="font-mono text-gray-800">{getShortId(childId)}...</span>
                      <span class="text-gray-600">View evaluation</span>
                    </a>
                  {/each}
                </div>
              </div>
            {:else}
              <div class="space-y-2">
                {#each group.loadedChildren as queryTree (queryTree.id)}
                  <ChildQueryTreeItem
                    {queryTree}
                    {baseUrl}
                    href={getChildLink(queryTree.id)}
                    {onNavigate}
                  />
                {/each}
              </div>

              <!-- Load more button or completion message for this group -->
              {#if group.loadedCount < group.childrenIds.length}
                <div class="mt-3 text-center">
                  <LoadMoreButton
                    onclick={() => loadMoreForGroup(group)}
                    loading={group.loadingMore}
                    loadedCount={group.loadedCount}
                    totalCount={group.childrenIds.length}
                    itemName="children"
                  />
                </div>
              {:else if group.childrenIds.length > BATCH_SIZE}
                <div class="mt-3 text-center text-xs text-gray-600">
                  All {group.childrenIds.length} children loaded
                </div>
              {/if}

              {#if group.error && group.loadedChildren.length > 0}
                <div class="mt-3 text-center text-xs text-red-600">
                  ⚠️ {group.error}
                </div>
              {/if}
            {/if}
          </div>
        {/each}
      </div>
    {/if}

    <div class="mt-3 text-xs text-gray-500">
      <p>
        💡 <strong>Tip:</strong> Click on any child evaluation above to navigate to it and see its details.
      </p>
    </div>
  </div>
{/if}
