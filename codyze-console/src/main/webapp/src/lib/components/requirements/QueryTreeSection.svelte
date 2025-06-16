<script lang="ts">
  import type { QueryTreeJSON } from '$lib/types';
  import QueryTreeExplorer from '../analysis/QueryTreeExplorer.svelte';

  interface Props {
    queryTree: QueryTreeJSON | undefined;
  }

  let { queryTree }: Props = $props();
</script>

{#if queryTree}
  <div class="mb-8">
    <h2 class="mb-4 text-xl font-semibold text-gray-900">Query Tree Analysis</h2>

    <div class="rounded-lg border bg-white p-4">
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
          (operations like AND, OR), UnaryOperationResult (operations like NOT), or QueryTree (single
          evaluations)
        </p>
      </div>

      <QueryTreeExplorer {queryTree} context="requirements" />
    </div>
  </div>
{:else}
  <div class="mb-8 rounded-lg border border-gray-200 bg-gray-50 p-6">
    <div class="text-center text-gray-500">
      <p class="text-lg font-medium">No Query Tree Available</p>
      <p class="mt-1 text-sm">
        This requirement has not been evaluated yet or the evaluation data is not available.
      </p>
    </div>
  </div>
{/if}
