<script lang="ts">
  import type { RequirementsCategoryJSON } from '$lib/types';
  import RequirementCard from '../requirements/RequirementCard.svelte';
  import { onMount } from 'svelte';

  interface Props {
    category: RequirementsCategoryJSON;
    initialExpanded?: boolean;
  }

  let { category, initialExpanded = false }: Props = $props();

  let isExpanded = $state(initialExpanded);

  // Expand if initialExpanded changes
  $effect(() => {
    if (initialExpanded) {
      isExpanded = true;
      // Scroll to this category after a short delay to allow for rendering
      setTimeout(() => {
        const element = document.getElementById(`category-${category.id}`);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    }
  });

  // Calculate category stats
  const stats = $derived({
    total: category.requirements.length,
    fulfilled: category.requirements.filter((r) => r.status === 'FULFILLED').length,
    notFulfilled: category.requirements.filter((r) => r.status === 'NOT_FULFILLED').length,
    rejected: category.requirements.filter((r) => r.status === 'REJECTED').length,
    undecided: category.requirements.filter((r) => r.status === 'UNDECIDED').length,
    notYetEvaluated: category.requirements.filter((r) => r.status === 'NOT_YET_EVALUATED').length
  });

  const fulfillmentRate = $derived(
    stats.total > 0 ? Math.round((stats.fulfilled / stats.total) * 100) : 0
  );
</script>

<div id="category-{category.id}" class="overflow-hidden rounded-lg border border-gray-200 bg-white">
  <!-- Category Header -->
  <button
    type="button"
    class="flex w-full items-center justify-between p-6 text-left transition-colors hover:bg-gray-50"
    onclick={() => (isExpanded = !isExpanded)}
  >
    <div class="flex-1">
      <div class="flex items-center gap-3">
        <h3 class="text-lg font-semibold text-gray-900">{category.name}</h3>
        <span
          class="inline-flex items-center rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800"
        >
          {category.requirements.length} requirements
        </span>
      </div>
      <p class="mt-1 text-sm text-gray-600">{category.description}</p>

      <!-- Quick stats -->
      <div class="mt-3 flex gap-4 text-xs">
        <span class="text-green-600">✓ {stats.fulfilled} fulfilled</span>
        <span class="text-red-600">✕ {stats.notFulfilled} not fulfilled</span>
        <span class="text-orange-600">⚠ {stats.rejected} rejected</span>
        <span class="text-yellow-600">? {stats.undecided} undecided</span>
        <span class="text-blue-600">⏳ {stats.notYetEvaluated} not yet evaluated</span>
        <span class="font-medium text-gray-700">({fulfillmentRate}% complete)</span>
      </div>
    </div>

    <div class="ml-4 flex items-center">
      <svg
        class="h-5 w-5 text-gray-400 transition-transform duration-200 {isExpanded
          ? 'rotate-180'
          : ''}"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </div>
  </button>

  <!-- Requirements List -->
  {#if isExpanded}
    <div class="border-t border-gray-200 bg-gray-50 p-6">
      <div class="space-y-3">
        {#each category.requirements as requirement (requirement.id)}
          <RequirementCard {requirement} />
        {/each}
      </div>
    </div>
  {/if}
</div>
