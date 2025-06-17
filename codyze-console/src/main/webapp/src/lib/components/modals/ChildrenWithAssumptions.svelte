<script lang="ts">
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { loadQueryTrees } from '$lib/stores/queryTreeStore';
  import type { QueryTreeJSON } from '$lib/types';
  import ChildQueryTreeItem from './ChildQueryTreeItem.svelte';

  interface Props {
    childrenIds: string[];
    baseUrl?: string;
    requirementId?: string;
    onNavigate?: () => void; // Callback to close modal before navigation
  }

  let { childrenIds, baseUrl, requirementId, onNavigate }: Props = $props();

  // State for loaded query trees
  let childrenQueryTrees = $state<QueryTreeJSON[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  // Load query tree data on mount
  onMount(async () => {
    if (childrenIds.length === 0) {
      loading = false;
      return;
    }

    try {
      loading = true;
      error = null;
      childrenQueryTrees = await loadQueryTrees(childrenIds);
    } catch (err) {
      console.error('Failed to load children query trees:', err);
      error = 'Failed to load child evaluation details';
    } finally {
      loading = false;
    }
  });

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
</script>

{#if childrenIds.length > 0}
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
    {:else if error}
      <div class="py-4 text-center text-sm text-red-600">
        <p>⚠️ {error}</p>
        <p class="mt-2 text-gray-600">Showing child IDs only:</p>
        <div class="mt-2 space-y-2">
          {#each childrenIds as childId}
            <a
              href={getChildLink(childId)}
              onclick={(e) => handleFallbackNavigation(e, childId)}
              class="block flex w-full items-center space-x-2 rounded border border-gray-200 bg-gray-50 px-3 py-2 text-left text-xs transition-colors hover:border-gray-300 hover:bg-gray-100"
            >
              <span class="font-mono text-gray-800">{getShortId(childId)}...</span>
              <span class="text-gray-600">View evaluation</span>
            </a>
          {/each}
        </div>
      </div>
    {:else}
      <div class="space-y-3">
        {#each childrenQueryTrees as queryTree (queryTree.id)}
          <ChildQueryTreeItem
            {queryTree}
            {baseUrl}
            href={getChildLink(queryTree.id)}
            {onNavigate}
          />
        {/each}
      </div>
    {/if}

    <div class="mt-3 text-xs text-gray-500">
      <p>
        💡 <strong>Tip:</strong> Click on any child evaluation above to navigate to it and see its assumptions.
      </p>
    </div>
  </div>
{/if}
