<script lang="ts">
  import type { RequirementJSON } from '$lib/types';

  interface Props {
    requirement: RequirementJSON;
  }

  let { requirement }: Props = $props();

  // Status styling configuration
  const statusConfig = {
    FULFILLED: {
      textColor: 'text-green-700',
      icon: '✓'
    },
    NOT_FULFILLED: {
      textColor: 'text-red-700',
      icon: '✕'
    },
    REJECTED: {
      textColor: 'text-orange-700',
      icon: '⚠'
    },
    UNDECIDED: {
      textColor: 'text-yellow-700',
      icon: '?'
    }
  };

  const config = $derived(
    statusConfig[requirement.status as keyof typeof statusConfig] || statusConfig.UNDECIDED
  );
</script>

<div class="rounded-lg border border-gray-200 bg-white p-6">
  <h3 class="mb-4 text-lg font-semibold text-gray-900">Evaluation Summary</h3>

  <dl class="space-y-3">
    <div class="flex justify-between">
      <dt class="text-sm font-medium text-gray-500">Status:</dt>
      <dd class="text-sm {config.textColor} font-medium">
        {config.icon}
        {requirement.status}
      </dd>
    </div>

    {#if requirement.queryTree}
      <div class="flex justify-between">
        <dt class="text-sm font-medium text-gray-500">Evaluation Result:</dt>
        <dd class="font-mono text-sm">
          {#if requirement.queryTree.nodeValues}
            <span class="font-semibold text-purple-600">
              {requirement.queryTree.nodeValues.length} node(s)
            </span>
          {:else}
            <span
              class="{requirement.queryTree.value === 'true'
                ? 'text-green-600'
                : 'text-red-600'} font-semibold"
            >
              {requirement.queryTree.value}
            </span>
          {/if}
        </dd>
      </div>

      <div class="flex justify-between">
        <dt class="text-sm font-medium text-gray-500">Confidence:</dt>
        <dd class="text-sm">
          {requirement.queryTree.confidence}
        </dd>
      </div>

      <div class="flex justify-between">
        <dt class="text-sm font-medium text-gray-500">Tree Depth:</dt>
        <dd class="text-sm">
          {requirement.queryTree.childrenIds && requirement.queryTree.childrenIds.length > 0
            ? 'Has sub-evaluations'
            : 'Leaf evaluation'}
        </dd>
      </div>
    {/if}
  </dl>
</div>
