<script lang="ts">
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { loadQueryTrees } from '$lib/stores/queryTreeStore';
  import type { QueryTreeJSON } from '$lib/types';
  import ChildQueryTreeItem from './ChildQueryTreeItem.svelte';

  interface Props {
    childrenIds: string[];
    baseUrl?: string;
    requirementId?: string;
  }

  let { childrenIds, baseUrl, requirementId }: Props = $props();

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
</script>

{#if childrenIds.length > 0}
  <div class="mt-4 border-t pt-4">
    <h4 class="text-sm font-semibold text-gray-900 mb-3">🔗 Propagated from Children</h4>
    <div class="text-xs text-gray-600 mb-3">
      <p>
        This result is influenced by assumptions made in the following child evaluations:
      </p>
    </div>
    
    {#if loading}
      <div class="flex items-center justify-center py-4">
        <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
        <span class="ml-2 text-sm text-gray-600">Loading child evaluations...</span>
      </div>
    {:else if error}
      <div class="py-4 text-center text-red-600 text-sm">
        <p>⚠️ {error}</p>
        <p class="mt-2 text-gray-600">Showing child IDs only:</p>
        <div class="space-y-2 mt-2">
          {#each childrenIds as childId}
            <a
              href={getChildLink(childId)}
              class="flex items-center space-x-2 rounded px-3 py-2 text-left text-xs bg-gray-50 border border-gray-200 hover:bg-gray-100 hover:border-gray-300 transition-colors w-full block"
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
