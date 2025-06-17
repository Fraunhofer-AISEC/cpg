<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import QueryTreeExplorer from '../analysis/QueryTreeExplorer.svelte';
  import { DashboardSection } from '../dashboard';
  import { loadQueryTreeWithParents } from '$lib/stores/queryTreeStore';
  import { onMount } from 'svelte';
  import { page } from '$app/stores';

  interface Props {
    queryTree: QueryTreeJSON | undefined;
    requirementId?: string;
    targetNodeId?: string; // ID of a specific node to expand to
  }

  let { queryTree, requirementId, targetNodeId }: Props = $props();

  let pathToTarget = $state<Set<string>>(new Set());
  let loadingTargetPath = $state(false);

  // Get clean relative URL for referrer (without targetNodeId parameter)
  const baseUrl = $derived(() => {
    if (typeof window === 'undefined') return '';
    const url = new URL(window.location.href);
    url.searchParams.delete('targetNodeId');
    return url.pathname + url.search;
  });

  // Load the path to target node if targetNodeId is provided
  onMount(async () => {
    if (targetNodeId && typeof window !== 'undefined') {
      loadingTargetPath = true;
      try {
        const result = await loadQueryTreeWithParents(targetNodeId);
        if (result) {
          // Create a set of all IDs on the path to the target
          const pathSet = new Set([targetNodeId, ...result.parentIds]);
          pathToTarget = pathSet;
        }
      } catch (error) {
        console.error('Failed to load path to target node:', error);
      } finally {
        loadingTargetPath = false;
      }
    }
  });

  // Reactive effect to handle targetNodeId changes during navigation
  $effect(() => {
    if (targetNodeId && typeof window !== 'undefined') {
      loadingTargetPath = true;
      
      // Handle async operation within the effect
      loadQueryTreeWithParents(targetNodeId)
        .then((result) => {
          if (result) {
            // Create a set of all IDs on the path to the target
            const pathSet = new Set([targetNodeId, ...result.parentIds]);
            pathToTarget = pathSet;
          }
        })
        .catch((error) => {
          console.error('Failed to load path to target node:', error);
        })
        .finally(() => {
          loadingTargetPath = false;
        });
    } else {
      // Clear path if no target
      pathToTarget = new Set();
    }
  });
</script>

{#if queryTree}
  <DashboardSection title="Query Tree Analysis">
    <div class="mb-4 text-sm text-gray-600">
      <p>
        This tree shows how the requirement was evaluated, including all logical operations and
        their results.
      </p>
      <p class="mt-1">
        <span class="font-medium">✓ Green:</span> Fulfilled (true and accepted),
        <span class="font-medium">✕ Red:</span> Not fulfilled (false and accepted),
        <span class="font-medium">⚠ Orange:</span> Rejected results,
        <span class="font-medium">? Yellow:</span> Undecided results,
        <span class="font-medium">• Gray:</span> Non-boolean values
      </p>
      <p class="mt-1">
        <span class="font-medium">📍 Blue sections:</span> Show where in the code each query was executed
        from
      </p>
      <p class="mt-1">
        <span class="font-medium">🔷 Blue badges:</span> Show the QueryTree type - BinaryOperationResult
        (operations like AND, OR), UnaryOperationResult (operations like NOT), or QueryTree (single evaluations)
      </p>
    </div>

    <QueryTreeExplorer
      {queryTree}
      context="requirements"
      {targetNodeId}
      {pathToTarget}
      baseUrl={baseUrl()}
    />
  </DashboardSection>
{:else}
  <DashboardSection title="Query Tree Analysis">
    <div class="py-8 text-center text-gray-500">
      <p class="text-lg font-medium">No Query Tree Available</p>
      <p class="mt-1 text-sm">
        This requirement has not been evaluated yet or the evaluation data is not available.
      </p>
    </div>
  </DashboardSection>
{/if}
